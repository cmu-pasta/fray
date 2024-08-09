plugins {
  java
  kotlin("jvm")
}

repositories {
  mavenCentral()
}

dependencies {
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  api("org.ow2.asm:asm:9.7")
  api("org.ow2.asm:asm-tree:9.7")
  api("org.ow2.asm:asm-commons:9.7")
  api("org.ow2.asm:asm-util:9.7")
  implementation(project(":runtime"))
}

tasks.withType<JavaExec> {
  jvmArgs("--patch-module", "org.pastalab.fray.instrumentation=${sourceSets["main"].output.asPath}")
}

tasks.compileJava {
  options.compilerArgumentProviders.add(CommandLineArgumentProvider {
    // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
    listOf("--patch-module", "org.pastalab.fray.instrumentation=${sourceSets["main"].output.asPath}")
  })
}


tasks.test {
  useJUnitPlatform()
}
