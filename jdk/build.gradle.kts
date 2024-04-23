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

tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into("${layout.buildDirectory.get().asFile}/libs")
}


tasks.register<Exec>("jlink") {
  var path = "${layout.buildDirectory.get().asFile}/libs"
  var jdkPath = "${layout.buildDirectory.get().asFile}/java-inst"
  /* delete(file(jdkPath)) */

  if (!File(jdkPath).exists()) {
    var runtimeJar = "$path/${project.name}-$version.jar"
    val jarDir = file(path)

    val jars = jarDir.listFiles { file -> file.extension == "jar" }
        ?.joinToString(separator = ":") { it.absolutePath }
      ?: "No JAR files found."
    val command = listOf("jlink", "-J-javaagent:$runtimeJar", "-J--module-path=$jars",
        "-J--add-modules=cmu.pasta.fray.jdk", "-J--class-path=$jars",
        "--output=$jdkPath", "--add-modules=ALL-MODULE-PATH",  "--fray-instrumentation")
    println(command.joinToString(" "))
    commandLine(command)
    /* commandLine(listOf("java", "--version")) */
    dependsOn(tasks.jar)
  }
}
