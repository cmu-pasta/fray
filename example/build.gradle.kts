plugins {
  id("java")
  id("org.pastalab.fray.gradle") version "0.2.9-SNAPSHOT"
}

group = "org.pastalab.fray.example"
version = "0.2.9-SNAPSHOT"

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
  useJUnitPlatform()
}
