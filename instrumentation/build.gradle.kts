import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  java
  kotlin("jvm")
  id("io.github.goooler.shadow") version "8.1.7"
}

repositories {
  mavenCentral()
}

dependencies {
  add("shadow", localGroovy())
  add("shadow", gradleApi())
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  api("org.ow2.asm:asm:9.7")
  api("org.ow2.asm:asm-tree:9.7")
  api("org.ow2.asm:asm-commons:9.7")
  api("org.ow2.asm:asm-util:9.7")
  implementation(project(":runtime"))
}

tasks.withType<JavaExec> {
  jvmArgs("--patch-module", "cmu.pasta.fray.instrumentation=${sourceSets["main"].output.asPath}")
}

tasks.compileJava {
  options.compilerArgumentProviders.add(CommandLineArgumentProvider {
    // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
    listOf("--patch-module", "cmu.pasta.fray.instrumentation=${sourceSets["main"].output.asPath}")
  })
}

tasks.jar {
  manifest {
    attributes(mapOf("Premain-Class" to "cmu.pasta.fray.instrumentation.PreMainKt"))
  }
}

tasks.named<ShadowJar>("shadowJar") {
  relocate("org.objectweb.asm", "cmu.pasta.fray.instrumentation.asm")
  manifest {
    attributes(mapOf("Premain-Class" to "cmu.pasta.fray.instrumentation.PreMainKt"))
  }
}

tasks.test {
  useJUnitPlatform()
}

tasks.named("build") {
  dependsOn("shadowJar")
}
