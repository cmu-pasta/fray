plugins {
  kotlin("jvm")
  id("org.gradlex.maven-plugin-development") version "1.0.2"
}

group = "org.pastalab.fray.maven"

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.apache.maven:maven-plugin-api:3.0")
  implementation("org.apache.maven.shared:maven-dependency-tree:3.3.0")
  implementation("org.apache.maven.shared:file-management:3.1.0")
  implementation("org.apache.maven.surefire:surefire-api:3.0.0")
  compileOnly("org.apache.maven:maven-core:3.0")
  compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:3.0")
  implementation(project(":plugins:base"))
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")

  implementation(project(":instrumentation:jdk"))
  /** This should be replaced with
  // project(":instrumentation:agent", configuration = "shadow")
  // after https://github.com/gradlex-org/maven-plugin-development/issues/315 is resolved
  **/
  implementation("org.pastalab.fray.instrumentation:fray-instrumentation-agent:0.2.1:shadow")
  implementation("org.pastalab.fray:fray-jvmti-linux-x86-64:0.2.0")
  implementation("org.pastalab.fray:fray-jvmti-macos-aarch64:0.2.0")
}

tasks.test {
  useJUnitPlatform()
}

tasks.named("generateMavenPluginDescriptor") {
  dependsOn(":plugins:base:shadowJar")
}

mavenPlugin {
  artifactId = "fray-plugins-maven"
}
