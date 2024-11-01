package com.pelotech.keycloak.mapper;

import freemarker.template.Configuration;
import java.util.List;
import org.keycloak.component.ComponentModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;

public class LdapTemplatedMapperFactory extends AbstractLDAPStorageMapperFactory {

  private static final LdapTemplatedMapperConfig config = new LdapTemplatedMapperConfig(
      "ldap.attribute",
      "from.ldap.template",
      "sso.attribute",
      "from.sso.template",
      "always.read.value.from.ldap");


  @Override
  public String getId() {
    return "ldap-templated-mapper";
  }

  @Override
  public String getHelpText() {
    return "Use Freemarker templates to map between ldap and sso attributes";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return ProviderConfigurationBuilder.create()
      .property()
      .name(config.getLdapAttributeName())
      .label("LDAP attribute")
      .helpText("Attribute in LDAP that is mapped to/from")
      .type(ProviderConfigProperty.STRING_TYPE)
      .add()
      .property()
      .name(config.getLdapTemplateName())
      .label("From LDAP Freemarker Template")
      .helpText("Freemarker template to transform values that come from LDAP")
      .type(ProviderConfigProperty.STRING_TYPE)
      .add()
      .property()
      .name(config.getSsoAttributeName())
      .label("SSO attribute")
      .helpText("Attribute in Keycloak that is mapped to/from")
      .type(ProviderConfigProperty.STRING_TYPE)
      .add()
      .property()
      .name(config.getSsoTemplateName())
      .label("From Keycloak Freemarker Template")
      .helpText("Freemarker template to transform values that come from Keycloak")
      .type(ProviderConfigProperty.STRING_TYPE)
      .add()
      .property()
      .name(config.getAlwaysReadFromLdapName())
      .label("Always Read Value From LDAP")
      .helpText("If on, then during reading of the LDAP attribute value will always used instead of the value from Keycloak DB")
      .type(ProviderConfigProperty.BOOLEAN_TYPE)
      .defaultValue("false")
      .add()
      .build();
  }

  @Override
  protected AbstractLDAPStorageMapper createMapper(ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
    Configuration freemarkerConfig = new Configuration(Configuration.VERSION_2_3_31);
    freemarkerConfig.setDefaultEncoding("UTF-8");

    return new LdapTemplatedMapper(mapperModel, federationProvider, config, freemarkerConfig);
  }
}
