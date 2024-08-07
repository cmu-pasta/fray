import java.util.regex.Pattern

plugins {
  kotlin("jvm") version "2.0.0"
  id("com.ncorti.ktfmt.gradle") version "0.17.0"
  id("maven-publish")
  id("org.jetbrains.dokka") version "1.9.20"
}


allprojects {
  group = "org.pastalab.fray"
  version = "0.1-SNAPSHOT"
}

repositories {
  mavenCentral()
}

dependencies {
  testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
  useJUnitPlatform()
}
kotlin {
  jvmToolchain(21)
}

configure(allprojects - project(":jvmti")) {
  plugins.apply("com.ncorti.ktfmt.gradle")
}

configure(allprojects - project("jvmti") - rootProject) {
  plugins.apply("maven-publish")
  plugins.apply("org.jetbrains.dokka")

  afterEvaluate {
    tasks.register<Jar>("dokkaJavadocJar") {
      dependsOn(tasks.dokkaJavadoc)
      from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
      archiveClassifier.set("javadoc")
    }
    java {
      withSourcesJar()
    }
    publishing {
      publications {
        create<MavenPublication>("maven") {
          pom {
            name = "Fray Testing Framework"
            description = "Fray testing framework for concurrency programs."
            url = "github.com/cmu-pasta/fray"
            licenses {
              license {
                name = "GPL-3.0"
                url = "https://www.gnu.org/licenses/gpl-3.0.html"
              }
            }

          }
          from(components["java"])
          artifact(tasks["dokkaJavadocJar"])
        }
      }
    }
  }
}
