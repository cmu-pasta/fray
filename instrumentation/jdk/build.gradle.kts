import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
  alias(libs.plugins.kotlin.jvm)
  java
}

dependencies {
  testImplementation(libs.kotlin.test)
  implementation(libs.kotlin.reflect)
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
            // This is a hack because gradle2nix does not support gradle 9 and thus cannot
            // use JDK 25. So when gradle was launched through `nix build`, it falls back to
            // JDK 21. We detect this situation through the JDK25 env variable and force
            // jlink to use JDK 25 from JDK25.
            val javaHome =
                System.getenv("JDK25")
                    ?: System.getenv("JAVA_HOME")
                    ?: throw Exception("JAVA_HOME is not set")
            val jlinkExe =
                if (getCurrentOperatingSystem().toFamilyName() == "windows") "jlink.exe"
                else "jlink"
            val command =
                listOf(
                    "$javaHome/bin/$jlinkExe",
                    "-J-javaagent:$runtimeJar",
                    "-J--module-path=$jars${File.pathSeparator}$runtimeJar",
                    "-J--add-modules=org.pastalab.fray.instrumentation.jdk",
                    "-J-Dfray.debug=true",
                    "-J--class-path=$jars${File.pathSeparator}$runtimeJar",
                    "--output=${jdkPath.absolutePath}",
                    "--add-modules=ALL-MODULE-PATH",
                    "--module-path=$javaHome/jmods",
                    "--release-info",
                    "add:IMPLEMENTOR=Fray",
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
