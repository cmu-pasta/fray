import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("com.gradle.plugin-publish") version "1.2.1"
  kotlin("jvm")
  id("java")
}

dependencies {
  implementation(project(":plugins:base"))
}

repositories {
  mavenCentral()
}

tasks.test {
  useJUnitPlatform()
}


gradlePlugin {
  website = "https://github.com/cmu-pasta/fray"
  vcsUrl = "https://github.com/cmu-pasta/fray"
  plugins {
    create("fray") {
      id = "org.pastalab.fray.gradle"
      displayName = "Fray Gradle Plugin"
      implementationClass = "org.pastalab.fray.gradle.FrayPlugin"
      description = "Fray gradle plugin to test concurrency programs."
      tags = listOf("fray", "testing", "concurrency")
    }
  }
}
