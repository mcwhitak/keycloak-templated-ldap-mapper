plugins {
  `java-library`
  `maven-publish`
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
  shadow(libs.bundles.keycloak)
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

publishing {
  repositories {
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/mcwhitak/keycloak-templated-ldap-mapper")
      credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("GITHUB_TOKEN")
      }
    }
  }
  publications {
    create<MavenPublication>("maven") {
        groupId = "com.pelotech"
        artifactId = "keycloak-templated-ldap-mapper"
        version = project.property("version").toString()

        from(components["java"])
    }
  }
}

tasks.named("build") {
  dependsOn(tasks.named("shadowJar"))
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}
