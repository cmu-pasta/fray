plugins {
  java
  application
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  api(libs.asm)
  api(libs.asm.tree)
  api(libs.asm.commons)
  api(libs.asm.util)
  testImplementation(libs.kotlin.test)
  implementation(libs.kotlin.reflect)
  implementation(project(":runtime"))
}

tasks.withType<JavaExec> {
  jvmArgs(
      "--patch-module",
      "org.pastalab.fray.instrumentation.base=${sourceSets["main"].output
      .asPath}",
  )
}

tasks.compileJava {
  options.compilerArgumentProviders.add(
      CommandLineArgumentProvider {
        // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
        listOf(
            "--patch-module",
            "org.pastalab.fray.instrumentation.base=${sourceSets["main"].output
        .asPath}",
        )
      }
  )
}

application { mainClass = "org.pastalab.fray.instrumentation.base.MainKt" }

tasks.test { useJUnitPlatform() }
