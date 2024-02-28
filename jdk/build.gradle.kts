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
    val command = listOf("/home/aoli/repos/corretto-21/build/linux-x86_64-server-fastdebug/images/jdk/bin/jlink", "-J-javaagent:$runtimeJar", "-J--module-path=$jars",
    /* val command = listOf("/home/aoli/repos/corretto-21-back/build/linux-x86_64-server-fastdebug/images/jdk/bin/jlink", "-J-javaagent:$runtimeJar", "-J--module-path=$jars", */
        "-J--add-modules=cmu.pasta.sfuzz.jdk", "-J--class-path=$jars",
        "--output=$jdkPath", "--add-modules=ALL-MODULE-PATH",  "--system-modules=batch-size=75", "--sfuzz-instrumentation")
    println(command.joinToString(" "))
    commandLine(command)
    /* commandLine(listOf("java", "--version")) */
    dependsOn(tasks.jar)
}