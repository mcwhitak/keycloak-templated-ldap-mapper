plugins {
  `java-library`
  alias(libs.plugins.spotless)
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

tasks.named("jar", Jar::class) {
  duplicatesStrategy = DuplicatesStrategy.INCLUDE
  from({
    configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
  })
}
