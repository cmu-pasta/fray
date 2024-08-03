plugins {
  id("java")
  kotlin("jvm")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(project(":core"))
  testCompileOnly(project(":runtime"))
  implementation("org.junit.platform:junit-platform-engine:1.10.3")
  implementation("org.junit.platform:junit-platform-commons:1.10.3")
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
}
