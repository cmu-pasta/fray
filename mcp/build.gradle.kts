plugins { alias(libs.plugins.kotlin.jvm) }

group = "org.pastalab.fray.mcp"

dependencies {
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)
  implementation(project(":rmi"))
  implementation(libs.mcp.sdk)
  implementation(libs.ktor.server.cio)
  implementation(libs.ktor.server.sse)
}

tasks.test { useJUnitPlatform() }
