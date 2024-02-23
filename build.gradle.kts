plugins {
    kotlin("jvm") version "1.9.22"
}


allprojects {
    group = "cmu.pasta.sfuzz"
    version = "1.0-SNAPSHOT"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}