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
  shadow(libs.keycloak.server.spi)
  shadow(libs.keycloak.server.spi.private)
  shadow(libs.keycloak.services)
  shadow(libs.keycloak.ldap.federation)
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


