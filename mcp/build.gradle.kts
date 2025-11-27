plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.shadow)
  application
}

group = "org.pastalab.fray.mcp"

application { mainClass.set("org.pastalab.fray.mcp.StandaloneMCPSchedulerKt") }

dependencies {
  implementation(project(":rmi"))
  implementation(libs.mcp.sdk)
  implementation(libs.ktor.server.cio)
  implementation(libs.ktor.server.sse)
  implementation(libs.asm)
  testRuntimeOnly(libs.junit.platform.launcher)
  testCompileOnly(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.junit.platform.testkit)
}

tasks.test { useJUnitPlatform() }
