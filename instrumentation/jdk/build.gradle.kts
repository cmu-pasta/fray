import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
  kotlin("jvm")
  java
}

dependencies {
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  implementation(kotlin("reflect"))
  implementation(project(":runtime"))
  implementation(project(":instrumentation:base"))
}

tasks.test { useJUnitPlatform() }

tasks.compileJava {
  options.compilerArgumentProviders.add(
      CommandLineArgumentProvider {
        // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
        listOf(
            "--patch-module",
            "org.pastalab.fray.instrumentation.jdk=${sourceSets["main"].output
            .asPath}")
      })
}

tasks.jar {
  manifest {
    attributes(mapOf("Premain-Class" to "org.pastalab.fray.instrumentation.jdk.agent.AgentKt"))
  }
  dependsOn("copyDependencies")
}

tasks.build {
  dependsOn("jar")
  val path = layout.buildDirectory.get().asFile.resolve("dependency")
  val jdkPath = layout.buildDirectory.get().asFile.resolve("java-inst")
  outputs.dirs(jdkPath)
  doLast {
    if (!state.upToDate) {
      providers
          .exec {
            if (jdkPath.exists()) {
              delete(jdkPath)
            }
            var runtimeJar =
                layout.buildDirectory
                    .get()
                    .asFile
                    .resolve("libs")
                    .resolve("${base.archivesName.get()}-$version.jar")
                    .absolutePath

            val jars =
                path
                    .listFiles { file -> file.extension == "jar" }
                    ?.joinToString(separator = File.pathSeparator) { it.absolutePath }
                    ?: "No JAR files found."
            val jlinkExe =
                if (getCurrentOperatingSystem().toFamilyName() == "windows") "jlink.exe"
                else "jlink"
            val command =
                listOf(
                    jlinkExe,
                    "-J-javaagent:$runtimeJar",
                    "-J--module-path=$jars${File.pathSeparator}$runtimeJar",
                    "-J--add-modules=org.pastalab.fray.instrumentation.jdk",
                    "-J-Dfray.debug=true",
                    "-J--class-path=$jars${File.pathSeparator}$runtimeJar",
                    "--output=${jdkPath.absolutePath}",
                    "--add-modules=ALL-MODULE-PATH",
                    "--module-path=${System.getenv("JAVA_HOME")}/jmods",
                    "--fray-instrumentation")
            println(command.joinToString(" "))
            commandLine(command)
          }
          .result
          .get()
    }
  }
}

tasks.register<Copy>("copyDependencies") {
  from(configurations.runtimeClasspath)
  into(layout.buildDirectory.get().asFile.resolve("dependency"))
}
