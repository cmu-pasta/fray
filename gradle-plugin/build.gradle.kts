plugins {
  id("java")
  kotlin("jvm")
  id("java-gradle-plugin")
  id("maven-publish")
  id("com.gradle.plugin-publish") version "1.2.1"
}

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {
}

tasks.test {
  useJUnitPlatform()
}

gradlePlugin {
  website = "https://example.com"
  vcsUrl = "https://example.com"
  plugins {
    create("fray") {
      id = "cmu.pasta.fray.gradle"
      displayName = "Fray Gradle Plugin"
      implementationClass = "cmu.pasta.fray.gradle.FrayPlugin"
      description = "Fray Gradle Plugin"
      tags = listOf("fray", "gradle", "testing")
    }
  }
}
