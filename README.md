# Keycloak Templated LDAP Mapper
This project contains an LDAP User Attribute Mapper for Keycloak that allows the injection of Freemarker templates for post-processing. 

This is meant to solve use cases where attributes do not have an explicit 1:1 mapping and instead there must be some translation (negating a boolean, sanitizing input, etc).

## Usage
TODO

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