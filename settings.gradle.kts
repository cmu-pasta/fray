plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "fray"
include("jdk")
include("runtime")
include("examples")
include("jvmti")
include("core")
include("instrumentation")
include("benchmarks")
include("integration-tests")
include("junit-analyzer")
