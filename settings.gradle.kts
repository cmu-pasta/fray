pluginManagement {
  plugins {
    id("com.ncorti.ktfmt.gradle").version("0.25.0")
    id("org.jetbrains.dokka").version("2.1.0")
    id("org.jetbrains.dokka-javadoc").version("2.1.0")
  }
  repositories { gradlePluginPortal() }
}

dependencyResolutionManagement { repositories { mavenCentral() } }

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

rootProject.name = "fray"

include("runtime")

include("jvmti")

include("core")

include("junit")

include("instrumentation:base")

include("instrumentation:agent")

include("instrumentation:jdk")

include("integration-test")

include("rmi")

include("mcp")

include("plugins:gradle")

include("plugins:idea")

include("plugins:maven")

include("plugins:base")
