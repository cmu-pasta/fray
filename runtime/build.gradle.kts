plugins {
  java
  kotlin("jvm")
}

repositories {
  mavenCentral()
}

dependencies {
}

tasks.test {
  useJUnitPlatform()
}
