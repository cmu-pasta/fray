plugins {
  java
  application
  kotlin("jvm")
}

dependencies {
  api("org.ow2.asm:asm:9.9")
  api("org.ow2.asm:asm-tree:9.9")
  api("org.ow2.asm:asm-commons:9.9")
  api("org.ow2.asm:asm-util:9.9")
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation(project(":runtime"))
}

tasks.withType<JavaExec> {
  jvmArgs(
      "--patch-module",
      "org.pastalab.fray.instrumentation.base=${sourceSets["main"].output
      .asPath}")
}

tasks.compileJava {
  options.compilerArgumentProviders.add(
      CommandLineArgumentProvider {
        // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
        listOf(
            "--patch-module",
            "org.pastalab.fray.instrumentation.base=${sourceSets["main"].output
        .asPath}")
      })
}

application { mainClass = "org.pastalab.fray.instrumentation.base.MainKt" }

tasks.test { useJUnitPlatform() }
