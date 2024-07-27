import org.gradle.internal.impldep.org.apache.commons.io.output.ByteArrayOutputStream

plugins {
    kotlin("jvm")
    java
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation(project(":runtime"))
    implementation(project(":instrumentation"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileJava {
    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
        // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
        listOf("--patch-module", "cmu.pasta.fray.jdk=${sourceSets["main"].output.asPath}")
    })
}

tasks.jar {
    manifest {
        attributes(mapOf("Premain-Class" to "cmu.pasta.fray.jdk.agent.AgentKt"))
    }
    dependsOn("copyDependencies")
}

tasks.build {
  dependsOn("jar")
  val path = "${layout.buildDirectory.get().asFile}/libs"
  val jdkPath = "${layout.buildDirectory.get().asFile}/java-inst"
  outputs.dirs(jdkPath)
  doLast {
    println(state)
    if (!state.upToDate) {
      exec {
        if (File(jdkPath).exists()) {
          delete(file(jdkPath))
        }
        var runtimeJar = "$path/${project.name}-$version.jar"
        val jarDir = file(path)

        val jars = jarDir.listFiles { file -> file.extension == "jar" }
            ?.joinToString(separator = ":") { it.absolutePath }
          ?: "No JAR files found."
        val command = listOf("jlink", "-J-javaagent:$runtimeJar", "-J--module-path=$jars",
            "-J--add-modules=cmu.pasta.fray.jdk", "-J--class-path=$jars",
            "--output=$jdkPath", "--add-modules=ALL-MODULE-PATH",  "--fray-instrumentation")
        commandLine(command)
      }
    }
  }
}

tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into("${layout.buildDirectory.get().asFile}/libs")
}
