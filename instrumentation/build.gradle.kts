import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    add("shadow", localGroovy())
    add("shadow", gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    implementation(project(":runtime"))
}

tasks.compileJava {
    println(sourceSets["main"].output.asPath)
    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
        // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
        listOf("--patch-module", "cmu.pasta.sfuzz.instrumentation=${sourceSets["main"].output.asPath}")
    })
}

tasks.jar {
    manifest {
        attributes(mapOf("Premain-Class" to "cmu.pasta.sfuzz.instrumentation.PreMainKt"))
    }
}

tasks.named<ShadowJar>("shadowJar") {
    // In Kotlin DSL, setting properties is done through Kotlin property syntax.
//    isEnableRelocation = true
    manifest {
        attributes(mapOf("Premain-Class" to "cmu.pasta.sfuzz.instrumentation.PreMainKt"))
    }
}

tasks.test {
    useJUnitPlatform()
}
