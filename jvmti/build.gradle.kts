import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

val os = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
val arch = DefaultNativePlatform.getCurrentArchitecture().name
val currentTarget = configurations.create("${os}-${arch}")


plugins {
  java
  id("io.github.tomtzook.gradle-cmake") version "1.2.2"
  id("maven-publish")
}

cmake {
  targets {
    register("native_release") { cmakeLists.set(file("src/CMakeLists.txt"))
      cmakeArgs.add("-DCMAKE_BUILD_TYPE=Release")
    }
  }
}

tasks.create<Jar>("sourcesJar") {
  archiveClassifier.set("sources")
  from("src/cpp")
}

tasks.named("clean") {
  dependsOn("cmakeClean")
}

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
  eachFile {
    path = path.substringAfterLast("/")
  }
  includeEmptyDirs = false
}


tasks.named<Jar>("jar") {
  dependsOn("collectNativeLibs")
  destinationDirectory.set(file("${layout.buildDirectory.get().asFile}/libs"))
  archiveClassifier.set("$os-$arch")
  from("${layout.buildDirectory.get().asFile}/native-libs") {
    include("**/*.so")
    include("**/*.dylib")
    include("**/*.dll")
  }
}
