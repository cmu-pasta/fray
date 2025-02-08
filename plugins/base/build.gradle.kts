import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  kotlin("jvm")
  id("com.gradleup.shadow")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.apache.commons:commons-compress:1.27.1")
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.named<ShadowJar>("shadowJar") {
  archiveClassifier.set("")
  relocate("org.apache.commons", "org.pastalab.fray.apache.commons")
}

tasks.jar {
  enabled = false
}


tasks.test {
  useJUnitPlatform()
}
