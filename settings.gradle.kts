pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
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
