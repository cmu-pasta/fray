plugins {
  id("io.github.tomtzook.gradle-cmake") version "1.2.2"
}

cmake {
  targets {
    register("native_release") { cmakeLists.set(file("src/CMakeLists.txt"))
      cmakeArgs.add("-DCMAKE_BUILD_TYPE=Release")
    }
  }
}

tasks.register("clean") {
  dependsOn("cmakeClean")
}

tasks.register("build") {
  dependsOn("cmakeBuild")
}
