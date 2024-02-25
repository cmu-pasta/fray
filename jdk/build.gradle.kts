import org.gradle.internal.impldep.org.apache.commons.io.output.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "1.9.22"
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
kotlin {
    jvmToolchain(21)
}

tasks.compileJava {
    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
        // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
        listOf("--patch-module", "cmu.pasta.sfuzz.jdk=${sourceSets["main"].output.asPath}")
    })
}

tasks.jar {
    manifest {
        attributes(mapOf("Premain-Class" to "cmu.pasta.sfuzz.jdk.agent.AgentKt"))
    }

    dependsOn("copyDependencies")
}

tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into("${layout.buildDirectory.get().asFile}/libs")
}


tasks.register<Exec>("jlink") {
    var path = "${layout.buildDirectory.get().asFile}/libs"
    var jdkPath = "${layout.buildDirectory.get().asFile}/jdk"
    delete(file(jdkPath))
    var runtimeJar = "$path/${project.name}-$version.jar"
    val jarDir = file(path)

    val jars = jarDir.listFiles { file -> file.extension == "jar" }
        ?.joinToString(separator = ":") { it.absolutePath }
        ?: "No JAR files found."
    println(listOf("jlink", "-J-javaagent:$runtimeJar", "-J--module-path=$jars",
        "-J--add-modules=cmu.pasta.sfuzz.jdk", "-J--class-path=$jars",
        "--output=$jdkPath", "--add-modules=ALL-MODULE-PATH").joinToString(" "))
    commandLine(listOf("jlink", "-J-javaagent:$runtimeJar", "-J--module-path=$jars",
        "-J--add-modules=cmu.pasta.sfuzz.jdk", "-J--class-path=$jars",
        "--output=$jdkPath", "--add-modules=ALL-MODULE-PATH", "--sfuzz-instrumentation"))
    dependsOn(tasks.jar)
}