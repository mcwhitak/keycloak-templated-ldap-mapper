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
  implementation(libs.bundles.keycloak)
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

      targets {
          all {
              testTask.configure {
                val jarFile = tasks.named("jar").get().getOutputs().getFiles().getSingleFile()
                doFirst {
                  environment("JAR_FILE", jarFile)
                }
              }
          }
      }
    }
  }
}

spotless {
  java {
    palantirJavaFormat()
  }
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}
