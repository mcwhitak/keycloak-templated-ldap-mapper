package com.pelotech.keycloak.mapper;

import com.pelotech.keycloak.mapper.LdapTemplatedMapperConfig;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.models.utils.UserModelDelegate;

public class LdapTemplatedMapper extends UserAttributeLDAPStorageMapper {

  private static final Logger logger = Logger.getLogger(LdapTemplatedMapper.class);

  private final LdapTemplatedMapperConfig config;
  private final Configuration freemarkerConfig;

  public LdapTemplatedMapper(ComponentModel model, LDAPStorageProvider provider, LdapTemplatedMapperConfig config, Configuration freemarkerConfig) {
    super(model, provider);
    this.config = config;
    this.freemarkerConfig = freemarkerConfig;
  }

  private String getStringConfig(String configName) {
    return mapperModel.getConfig().getFirst(configName);
  }

  @Override
  public void onImportUserFromLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm, boolean isCreate) {

    boolean isBinaryAttribute = mapperModel.get(UserAttributeLDAPStorageMapper.IS_BINARY_ATTRIBUTE, false);
    if (isBinaryAttribute) {
        return;
    }

    String ldapTemplate = getStringConfig(config.getLdapTemplateName());

    String ldapAttribute = getStringConfig(config.getLdapAttributeName());
    String rawLdapValue = ldapUser.getAttributeAsString(ldapAttribute);

    String result = runTemplate("ldap", ldapTemplate, rawLdapValue);

    String ssoAttribute = getStringConfig(config.getSsoAttributeName());
    logger.debugf("Setting sso attribute '%s' on user '%s' from ldap attribute '%s' with value '%s' transformed from '%s'",
        ssoAttribute, user.getUsername(), ldapAttribute, result, rawLdapValue);
    user.setSingleAttribute(ssoAttribute, result);
  }

  @Override
  public void onRegisterUserToLDAP(LDAPObject ldapUser, UserModel user, RealmModel realm) {
    String ssoTemplate = getStringConfig(config.getSsoTemplateName());

    String ssoAttribute = getStringConfig(config.getSsoAttributeName());
    List<String> rawSsoList = user.getAttributes().get(ssoAttribute);

    // Collapse values into comma separated string
    String rawSsoValue = String.join(",", rawSsoList);
    String result = runTemplate("sso", ssoTemplate, rawSsoValue);

    String ldapAttribute = getStringConfig(config.getLdapAttributeName());
    logger.debugf("Setting ldap attribute '%s' on user '%s' from sso attribute '%s' with value '%s' transformed from '%s'",
        ldapAttribute, user.getUsername(), ssoAttribute, result, rawSsoValue);
    ldapUser.setSingleAttribute(ldapAttribute, result);

    if (isReadOnly()) {
        ldapUser.addReadOnlyAttributeName(ldapAttribute);
    }
  }

  private String runTemplate(String source, String templateString, String input) {
    Map<String, String> root = new HashMap<>();
    root.put(source + "Value", input);

    try {
      Template template = new Template(source + "Template", templateString, freemarkerConfig);

      StringWriter out = new StringWriter();
      template.process(root, out);

      return out.toString();
    } catch (IOException | TemplateException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public UserModel proxy(LDAPObject ldapUser, UserModel delegate, RealmModel realm) {
    String ldapAttribute = getStringConfig(config.getLdapAttributeName());
    String ldapTemplate = getStringConfig(config.getLdapTemplateName());
    String ssoAttribute = getStringConfig(config.getSsoAttributeName());
    String ssoTemplate = getStringConfig(config.getSsoTemplateName());
    boolean isAlwaysReadValueFromLDAP = parseBooleanParameter(mapperModel, ALWAYS_READ_VALUE_FROM_LDAP);

    UserModel proxy = super.proxy(ldapUser, delegate, realm);

    // Write through logic on changes to keycloak directly
    if (ldapProvider.getEditMode() == UserStorageProvider.EditMode.WRITABLE && !isReadOnly()) {
      proxy = new UserModelDelegate(proxy) {
            @Override
            public void setSingleAttribute(String name, String value) {
                String finalValue = value;
                if (name.equals(ssoAttribute)) {
                  finalValue = runTemplate("sso", ssoTemplate, value);
                }
                super.setSingleAttribute(name, finalValue);
            }

            @Override
            public void setAttribute(String name, List<String> values) {
                if (name.equals(ssoAttribute)) {
                  String concatValue = String.join(",", values);

                  String result = runTemplate("sso", ssoTemplate, concatValue);

                  super.setAttribute(name, Arrays.asList(result.split(",")));
                  this.setSingleAttribute(name, concatValue);
                } else {
                  super.setAttribute(name, values);
                }
            }

            @Override
            public void setLastName(String lastName) {
                String finalValue = lastName;
                if(ssoAttribute.equals(UserModel.LAST_NAME)) {
                  finalValue = runTemplate("sso", ssoTemplate, lastName);
                }
                super.setLastName(finalValue);
            }

            @Override
            public void setFirstName(String firstName) {
                String finalValue = firstName;
                if (ssoAttribute.equals(UserModel.FIRST_NAME)) {
                  finalValue = runTemplate("sso", ssoTemplate, firstName);
                }
                super.setFirstName(finalValue);
            }
      };
    }

    // If never storing attributes locally, run template dynamically from FreeIPA
    // WARNING: This will not be great for performance as the template will run every single time
    if (isAlwaysReadValueFromLDAP) {
      proxy = new UserModelDelegate(proxy) {
            @Override
            public String getFirstAttribute(String name) {
                if (name.equalsIgnoreCase(ssoAttribute)) {
                  String valueBeforeTemplating = ldapUser.getAttributeAsString(ldapAttribute);
                  return runTemplate("ldap", ldapTemplate, name);
                }

                return super.getFirstAttribute(name);
            }

            @Override
            public List<String> getAttribute(String name) {
                if (name.equalsIgnoreCase(ssoAttribute)) {
                  Collection<String> values = ldapUser.getAttributeAsSet(ldapAttribute);
                  if (values == null) {
                    return Collections.emptyList();
                  }

                  String concatValue = String.join(",", values);
                  String finalValue = runTemplate("ldap", ldapTemplate, concatValue);

                  return Arrays.asList(finalValue.split(","));
                }

                return super.getAttribute(name);
            }

            @Override
            public Map<String, List<String>> getAttributes() {
                Map<String, List<String>> attrs = new HashMap<>(super.getAttributes());

                //Ignore username, email, firstname, lastname
                //These values *are* written to the Keycloak DB regardless of a "read only" flag
                if (UserModel.FIRST_NAME.equalsIgnoreCase(ssoAttribute) ||
                    UserModel.LAST_NAME.equalsIgnoreCase(ssoAttribute) ||
                    UserModel.EMAIL.equalsIgnoreCase(ssoAttribute) ||
                    UserModel.USERNAME.equalsIgnoreCase(ssoAttribute)) {
                  return attrs;
                }

                Set<String> attributeValues = ldapUser.getAttributeAsSet(ldapAttribute);
                if (attributeValues != null) {
                  String concatValue = String.join(",", attributeValues);
                  String finalValue = runTemplate("ldap", ldapTemplate, concatValue);

                  List<String> finalValues = Arrays.asList(finalValue.split(","));
                  attrs.put(ssoAttribute, finalValues);
                }

                return attrs;
            }

            @Override
            public String getEmail() {
                if (UserModel.EMAIL.equalsIgnoreCase(ssoAttribute)) {
                    String email = ldapUser.getAttributeAsString(ldapAttribute);
                    return runTemplate("ldap", ldapTemplate, email);
                }
                return super.getEmail();
            }

            @Override
            public String getLastName() {
                if (UserModel.LAST_NAME.equalsIgnoreCase(ssoAttribute)) {
                    String lastName = ldapUser.getAttributeAsString(ldapAttribute);
                    return runTemplate("ldap", ldapTemplate, lastName);
                }
                return super.getLastName();
            }

            @Override
            public String getFirstName() {
                if (UserModel.FIRST_NAME.equalsIgnoreCase(ssoAttribute)) {
                    String firstName = ldapUser.getAttributeAsString(ldapAttribute);
                    return runTemplate("ldap", ldapTemplate, firstName);
                }
                return super.getFirstName();
            }
      };
    }

    return proxy;
  }

  private boolean isReadOnly() {
      return parseBooleanParameter(mapperModel, READ_ONLY);
  }
}
