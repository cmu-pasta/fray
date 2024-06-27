import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
plugins {
  kotlin("jvm")
  id("io.github.goooler.shadow") version "8.1.7"
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly("junit:junit:4.13.2")
  compileOnly("org.junit.platform:junit-platform-engine:1.10.2")
  compileOnly("org.junit.platform:junit-platform-launcher:1.10.2")
  compileOnly("org.junit.platform:junit-platform-console-standalone:1.10.2")
  compileOnly(project(":runtime"))
  implementation(project(":instrumentation"))
  implementation(kotlin("stdlib-jdk8"))
  add("shadow", localGroovy())
  add("shadow", gradleApi())
}

tasks.test {
  useJUnitPlatform()
}
kotlin {
  jvmToolchain(21)
}
tasks.named<ShadowJar>("shadowJar") {
  // In Kotlin DSL, setting properties is done through Kotlin property syntax.
//    isEnableRelocation = true
  relocate("org.objectweb.asm", "cmu.pasta.fray.instrumentation.asm")
  manifest {
    attributes(mapOf("Premain-Class" to "cmu.pasta.fray.junit.RecorderKt"))
  }
}
