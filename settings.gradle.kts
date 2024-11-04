dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven {
      url = uri("https://maven.repository.redhat.com/ga/")
    }
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "keycloak-templated-ldap-mapper"
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
