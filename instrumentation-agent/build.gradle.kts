import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
plugins {
  id("java")
  kotlin("jvm")
  id("io.github.goooler.shadow") version "8.1.7"
}

repositories {
  mavenCentral()
}

dependencies {
  add("shadow", localGroovy())
  add("shadow", gradleApi())
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  implementation(project(":instrumentation"))
}

tasks.named<ShadowJar>("shadowJar") {
  archiveClassifier.set("shadow")
  relocate("org.objectweb.asm", "org.pastalab.fray.instrumentation.agent.asm")
  dependencies {
    exclude(project(":runtime"))
  }
  manifest {
    attributes(mapOf("Premain-Class" to "org.pastalab.fray.instrumentation.agent.PreMainKt"))
  }
}

tasks.test {
  useJUnitPlatform()
}
