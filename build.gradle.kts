import java.util.regex.Pattern

plugins {
  kotlin("jvm") version "2.0.0"
  id("com.ncorti.ktfmt.gradle") version "0.17.0"
  id("maven-publish")
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
  afterEvaluate {
    java {
      withSourcesJar()
    }
    publishing {
      publications {
        create<MavenPublication>("fray") {
          from(components["java"])
        }
      }
    }
  }
}
