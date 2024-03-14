plugins {
    id("io.github.tomtzook.gradle-cmake") version "1.2.2"
}

cmake {
    targets {
        register("native_release") {
            cmakeLists.set(file("src/CMakeLists.txt"))
            cmakeArgs.add("-DCMAKE_BUILD_TYPE=Release")
        }
        register("native_debug") {
            cmakeLists.set(file("src/CMakeLists.txt"))
            cmakeArgs.add("-DCMAKE_BUILD_TYPE=Debug")
        }
    }
}

tasks.register("clean") {
    dependsOn("cmakeClean")
}

tasks.register("build") {
    dependsOn("cmakeBuild")
}


//val suffix = if (machines.host.name.contains("macos")) {
//    ".dylib"
//} else {
//    ".so"
//}
//
//"${layout.buildDirectory.get().asFile}/cmake/native_release/${machines.host.name}/cpp/lib$name$suffix"