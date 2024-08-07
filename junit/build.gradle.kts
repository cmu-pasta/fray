plugins {
  id("java")
  kotlin("jvm")
  kotlin("plugin.serialization") version "2.0.0"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(project(":core"))
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
  implementation("org.junit.platform:junit-platform-engine:1.10.3")
  implementation("org.junit.platform:junit-platform-commons:1.10.3")
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
}
