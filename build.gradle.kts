import java.util.regex.Pattern

plugins {
  kotlin("jvm") version "1.9.22"
  id("com.ncorti.ktfmt.gradle") version "0.17.0"
  id("maven-publish")
}


allprojects {
  group = "cmu.pasta.fray"
  version = "1.0"
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
extra["agentPath"] = if (System.getProperty("os.name").lowercase().contains("mac")) {
  "${jvmti.layout.buildDirectory.get().asFile}/cmake/native_release/mac-aarch64/cpp/lib${jvmti.name}.dylib"
} else {
  "${jvmti.layout.buildDirectory.get().asFile}/cmake/native_release/linux-amd64/cpp/lib${jvmti.name}.so"
}

configure(allprojects - project(":jvmti")) {
  plugins.apply("com.ncorti.ktfmt.gradle")
}

configure(allprojects - project("jvmti") - project(":gradle-plugin") - rootProject) {
  plugins.apply("maven-publish")
  afterEvaluate {
    publishing {
      publications {
        create<MavenPublication>("fray") {
          from(components["java"])
        }
      }
    }
  }
}
