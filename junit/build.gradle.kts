plugins {
  id("java")
  kotlin("jvm")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(project(":core"))
  testCompileOnly(project(":runtime"))
  implementation("org.junit.platform:junit-platform-engine:1.10.3")
  implementation("org.junit.platform:junit-platform-commons:1.10.3")
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
  useJUnitPlatform {
    includeEngines("fray")
  }
  val agentPath: String by rootProject.extra
  val jdk = project(":jdk")
  val instrumentation = project(":instrumentation").tasks.named("shadowJar").get().outputs.files.first().absolutePath
  executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
  jvmArgs("-agentpath:$agentPath")
  jvmArgs("-javaagent:$instrumentation")
  dependsOn(":jdk:build")
  dependsOn(":jvmti:build")
}
