package com.pelotech.keycloak.mapper;

import com.pelotech.keycloak.mapper.LdapTemplatedMapperConfig;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.idm.query.internal.LDAPQuery;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.models.utils.UserModelDelegate;

public class LdapTemplatedMapper extends AbstractLDAPStorageMapper {

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
    return delegate;
  }



  @Override
  public void beforeLDAPQuery(LDAPQuery query) {
    String ldapAttribute = getStringConfig(config.getLdapAttributeName());
    query.addReturningLdapAttribute(ldapAttribute);
  }
}
