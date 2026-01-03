import org.gradle.kotlin.dsl.register

plugins {
  java
  alias(libs.plugins.cmake)
  id("maven-publish")
}

cmake {
  targets {
    register(
        "native_release",
        Action {
          cmakeLists.set(file("src/CMakeLists.txt"))
          cmakeArgs.add("-DCMAKE_BUILD_TYPE=Release")
        },
    )
  }
}

tasks.register<Jar>("sourcesJar") {
  archiveClassifier.set("sources")
  from("src/cpp")
}

tasks.named("clean") { dependsOn("cmakeClean") }

tasks.register<Copy>("collectNativeLibs") {
  dependsOn("cmakeBuild")
  from(
      fileTree("${layout.buildDirectory.get().asFile}/cmake/native_release") {
        include("**/*.so")
        include("**/*.dylib")
        include("**/*.dll")
      },
  )
  into("${layout.buildDirectory.get().asFile}/native-libs")
  eachFile { path = path.substringAfterLast("/") }
  includeEmptyDirs = false
}

tasks.named<Jar>("jar") {
  dependsOn("collectNativeLibs")
  destinationDirectory.set(file("${layout.buildDirectory.get().asFile}/libs"))
  from("${layout.buildDirectory.get().asFile}/native-libs") {
    include("**/*.so")
    include("**/*.dylib")
    include("**/*.dll")
  }
}
