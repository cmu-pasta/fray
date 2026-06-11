plugins {
  id("java")
  id("org.pastalab.fray.gradle")
}

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {
  testImplementation("junit:junit:4.13")
}

tasks.test {
}
