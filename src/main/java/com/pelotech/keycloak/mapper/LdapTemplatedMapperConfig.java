package com.pelotech.keycloak.mapper;

public class LdapTemplatedMapperConfig {
    private final String ldapAttributeName;
    private final String ldapTemplateName;
    private final String ssoAttributeName;
    private final String ssoTemplateName;

    public LdapTemplatedMapperConfig(
            String ldapAttributeName, String ldapTemplateName, String ssoAttributeName, String ssoTemplateName) {
        this.ldapAttributeName = ldapAttributeName;
        this.ldapTemplateName = ldapTemplateName;
        this.ssoAttributeName = ssoAttributeName;
        this.ssoTemplateName = ssoTemplateName;
    }

    public String getLdapAttributeName() {
        return this.ldapAttributeName;
    }

    public String getLdapTemplateName() {
        return this.ldapTemplateName;
    }

    public String getSsoAttributeName() {
        return this.ssoAttributeName;
    }

    public String getSsoTemplateName() {
        return this.ssoTemplateName;
    }
}
