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

val jvmti = project(":jvmti")
extra["agentPath"] = if (System.getProperty("os.name").toLowerCase().contains("mac")) {
    "${jvmti.layout.buildDirectory.get().asFile}/cmake/native_release/mac-aarch64/cpp/lib${jvmti.name}.dylib"
} else {
    "${jvmti.layout.buildDirectory.get().asFile}/cmake/native_release/linux-amd64/cpp/lib${jvmti.name}.so"
}
