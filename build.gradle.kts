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

testing {
  suites {
    register<JvmTestSuite>("integrationTest") {
      useJUnitJupiter()

      dependencies {
        implementation(project())
        implementation(libs.keycloak.admin.client)
        implementation.bundle(libs.bundles.testcontainers)
      }
    }
  }
}

dependencies {
  implementation(libs.freemarker)
  shadow(libs.bundles.keycloak)
}


spotless {
  java {
    palantirJavaFormat()
  }
}

tasks.named("build") {
  dependsOn(tasks.named("shadowJar"))
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}

tasks.named("integrationTest", Test::class) {
  val jarFile = tasks.named("jar").get().getOutputs().getFiles().getSingleFile()
  doFirst {
    environment("JAR_FILE", jarFile)
  }
}
