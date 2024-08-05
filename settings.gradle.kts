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
include("jdk")
include("runtime")
include("junit-runner")
include("jvmti")
include("core")
include("instrumentation")
include("integration-tests")
include("junit-analyzer")
include("junit")
