package com.pelotech.keycloak.mapper;

import freemarker.template.Configuration;
import java.util.List;
import org.keycloak.component.ComponentModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.mappers.AbstractLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapperFactory;

public class LdapTemplatedMapperFactory extends UserAttributeLDAPStorageMapperFactory {

    private static final LdapTemplatedMapperConfig config = new LdapTemplatedMapperConfig(
            UserAttributeLDAPStorageMapper.USER_MODEL_ATTRIBUTE,
            "from.ldap.template",
            UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE,
            "from.sso.template");

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
        List<ProviderConfigProperty> baseConfig = super.getConfigProperties();

        List<ProviderConfigProperty> additionalConfig = ProviderConfigurationBuilder.create()
                .property()
                .name(config.getLdapTemplateName())
                .label("From LDAP Freemarker Template")
                .helpText("Freemarker template to transform values that come from LDAP")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .property()
                .name(config.getSsoTemplateName())
                .label("From Keycloak Freemarker Template")
                .helpText("Freemarker template to transform values that come from Keycloak")
                .type(ProviderConfigProperty.STRING_TYPE)
                .add()
                .build();

        baseConfig.addAll(additionalConfig);
        return baseConfig;
    }

    @Override
    protected AbstractLDAPStorageMapper createMapper(
            ComponentModel mapperModel, LDAPStorageProvider federationProvider) {
        Configuration freemarkerConfig = new Configuration(Configuration.VERSION_2_3_31);
        freemarkerConfig.setDefaultEncoding("UTF-8");

        return new LdapTemplatedMapper(mapperModel, federationProvider, config, freemarkerConfig);
    }
}
