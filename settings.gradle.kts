pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
  }
}
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
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
