# THIS IS CURRENTLY STILL A WIP DO NOT USE THIS

# Keycloak Templated LDAP Mapper
This project contains an LDAP User Attribute Mapper for Keycloak that allows the injection of Freemarker templates for post-processing. 

This is meant to solve use cases where attributes do not have an explicit 1:1 mapping and instead there must be some translation (negating a boolean, sanitizing input, etc).

## Usage
Download the JAR from the latest release and place in your keycloak installations `/providers` folder. Upon restart you should have the ability to select the `ldap-templated-mapper` from within the `User Federation` -> `Settings` -> `New Mapper` pane.

There are two templates that can be specified, one when values flow from Keycloak -> LDAP and another when values flow from LDAP -> Keycloak. When writing templates where Keycloak is the value source, the value will be available as `${ssoValue}` and when writing templates where LDAP is the value source the value is available as `${ldapValue}`. 

Within the `integrationTest` task there is an example of a conditional template that leverages these values:

```
from.ldap.template = <#if ldapValue ==\"true\">false<#else>true</#if>
from.sso.template = <#if ssoValue == \"true\">false<#else>true</#if>
```

The project was initially conceived for the specific purpose of overriding the mismatch between the ldap `nsaccountlock`flag and the keycloak `enabled` flag so for other reserved properties (email, firstName, etc) there may be some sharp edges. Feel free to make a pull request or log an issue if there is a feature you would like to see added or a bug you discover.

## Prerequisites

* Java 17+


This project is built with Java/Gradle. Java 17+ is required to run Gradle itself but we use dynamic toolchains to build the resulting JAR with JDK 8 (which will be downloaded automatically for you if a compatible JDK isn't found on your machine).

Running commands through the Gradle wrapper will automatically download the Gradle binaries as well so there should be no need to have anything else installed on your system.

## Building
This project commits to a "single entrypoint" style of build, so building the project will always be as simple as:

```
./gradlew build
```

### Formatting
We use spotless to format our code. The `build` task only checks that the formatting is correct, if you'd like to *apply* the formatting you can use the following task

```
./gradlew spotlessApply
```

## Acknowledgements
Much of the learning of how Keycloak Attribute mappers work was due to reading through the [Keycloak source code](https://github.com/keycloak/keycloak) as well as the following projects:

[keycloak-custom-ldap-enabled-mapper](https://github.com/Nithe14/keycloak-custom-ldap-enabled-mapper/tree/master) by [@Nithe14](https://github.com/Nithe14)
