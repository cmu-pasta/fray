pluginManagement {
  repositories {
    maven {
      url = uri("file://nix/store/rmdxr1v0sw2r98iq1gm3cn7n882dapjg-gradle-maven-repo")
    }
  }
}
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
rootProject.name = "fray"
include("runtime")
include("jvmti")
include("core")
include("junit")
include("instrumentation:base")
include("instrumentation:agent")
include("instrumentation:jdk")
include("integration-test")
include("plugins:gradle")
include("plugins:idea")
include("rmi")
include("plugins:maven")
include("plugins:base")
include("mcp")
