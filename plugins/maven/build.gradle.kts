plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.maven.plugin.development)
}

group = "org.pastalab.fray.maven"

dependencies {
  implementation(libs.maven.plugin.api)
  implementation(libs.maven.dependency.tree)
  implementation(libs.maven.file.management)
  implementation(libs.maven.surefire.api)
  compileOnly(libs.maven.core)
  compileOnly(libs.maven.plugin.annotations)
  testImplementation(platform(libs.junit.bom))
  testImplementation(libs.junit.jupiter)

  implementation(project(":instrumentation:jdk"))
  implementation(project(":plugins:base", configuration = "shadow"))
  implementation(project(":instrumentation:agent", configuration = "shadow"))
}

tasks.test { useJUnitPlatform() }

mavenPlugin { artifactId = "fray-plugins-maven" }
