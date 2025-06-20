plugins {
    kotlin("jvm")
    java
}

dependencies {
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  implementation(project(":runtime"))
  implementation(project(":instrumentation:base"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileJava {
    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
        // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
        listOf("--patch-module", "org.pastalab.fray.instrumentation.jdk=${sourceSets["main"].output
            .asPath}")
    })
}

tasks.jar {
    manifest {
        attributes(mapOf("Premain-Class" to "org.pastalab.fray.instrumentation.jdk.agent.AgentKt"))
    }
    dependsOn("copyDependencies")
}

fun buildJdk(basePath: String, frayJarPath: String, outputPath: String) {
  providers.exec {
    if (File(outputPath).exists()) {
      delete(file(outputPath))
    }
    val runtimeJar = "$frayJarPath/../libs/${base.archivesName.get()}-$version.jar"
    val jarDir = file(frayJarPath)

    val jars = jarDir.listFiles { file -> file.extension == "jar" }
        ?.joinToString(separator = ":") { it.absolutePath }
      ?: "No JAR files found."
    val command = listOf("$basePath/bin/jlink", "-J-javaagent:$runtimeJar", "-J--module-path=$jars:$runtimeJar",
        "-J--add-modules=org.pastalab.fray.instrumentation.jdk",
        "-J-Dfray.debug=true",
        "-J--class-path=$jars:$runtimeJar",
        "--output=$outputPath", "--add-modules=ALL-MODULE-PATH",  "--fray-instrumentation")
    println(command.joinToString(" "))
    commandLine(command)
  }.result.get()
}

tasks.build {
  dependsOn("jar")
  val frayJarPath = "${layout.buildDirectory.get().asFile}/dependency"
  val jdkPath = "${layout.buildDirectory.get().asFile}/java-inst"
  outputs.dirs(jdkPath)
  doLast {
    if (!state.upToDate) {
      buildJdk(
          basePath = System.getenv("JAVA_HOME"),
          frayJarPath = frayJarPath,
          outputPath = jdkPath
      )
      val jdk21 = System.getenv("JDK21")
      if (jdk21 != null) {
        buildJdk(
            basePath = jdk21,
            frayJarPath = frayJarPath,
            outputPath = "${layout.buildDirectory.get().asFile}/java-inst-jdk21"
        )
      }
    }
  }
}

tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into("${layout.buildDirectory.get().asFile}/dependency")
}
