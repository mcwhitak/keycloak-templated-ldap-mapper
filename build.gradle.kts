plugins {
  `java-library`
  alias(libs.plugins.spotless)
  alias(libs.plugins.shadow)
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(8)
  }
}

dependencies {
  implementation(libs.freemarker)
  implementation(libs.keycloak.server.spi)
  implementation(libs.keycloak.server.spi.private)
  implementation(libs.keycloak.services)
  implementation(libs.keycloak.ldap.federation)
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter()
    }
  }
}

spotless {
  java {
    palantirJavaFormat()
  }
}
