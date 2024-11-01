package com.pelotech.keycloak.mapper;

public class LdapTemplatedMapperConfig {
  private final String ldapAttributeName;
  private final String ldapTemplateName;
  private final String ssoAttributeName;
  private final String ssoTemplateName;
  private final String alwaysReadFromLdapName;

  public LdapTemplatedMapperConfig(
      String ldapAttributeName,
      String ldapTemplateName,
      String ssoAttributeName,
      String ssoTemplateName,
      String alwaysReadFromLdapName) {
    this.ldapAttributeName = ldapAttributeName;
    this.ldapTemplateName = ldapTemplateName;
    this.ssoAttributeName = ssoAttributeName;
    this.ssoTemplateName = ssoTemplateName;
    this.alwaysReadFromLdapName = alwaysReadFromLdapName;
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

  public String getAlwaysReadFromLdapName() {
    return this.alwaysReadFromLdapName;
  }

}
