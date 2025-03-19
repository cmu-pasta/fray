plugins {
  id("com.gradle.plugin-publish") version "1.2.1"
  kotlin("jvm") version "2.1.10"
  id("java")
}

dependencies {
  implementation(project(":plugins:base", configuration = "shadow"))
}

repositories {
  mavenCentral()
}

tasks.test {
  useJUnitPlatform()
}

gradlePlugin {
  website = "https://github.com/anon/fray"
  vcsUrl = "https://github.com/anon/fray"
  plugins {
    create("fray") {
      id = "org.anonlab.fray.gradle"
      displayName = "Fray Gradle Plugin"
      implementationClass = "org.anonlab.fray.gradle.FrayPlugin"
      description = "Fray gradle plugin to test concurrency programs."
      tags = listOf("fray", "testing", "concurrency")
    }
  }
}
