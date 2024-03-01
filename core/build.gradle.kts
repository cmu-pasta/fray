plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.22"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":runtime"))
    compileOnly(project(":instrumentation"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
