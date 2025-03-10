plugins {
  id("java")
  id("org.pastalab.fray.gradle") version "0.3.1-SNAPSHOT"
}

group = "org.pastalab.fray.example"

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
