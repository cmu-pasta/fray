plugins {
    kotlin("jvm") version "1.9.22"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
    implementation(project(":runtime"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks.compileJava {
    options.compilerArgs = listOf("--add-exports", "jdk.jlink/jdk.tools.jlink.plugin=ALL-UNNAMED")
}


tasks.jar {
    manifest {
        attributes(mapOf("Premain-Class" to "cmu.pasta.sfuzz.jdk.agent.AgentKt"))
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get().filter { it.isDirectory }.map { it } +
                configurations.runtimeClasspath.get().filter { !it.isDirectory }.map { zipTree(it) }
    })
}