plugins {
  id("java")
    kotlin("jvm")
}

group = "org.pastalab.fray.test"

repositories {
  mavenCentral()
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation(project(":core"))
  testImplementation("io.github.classgraph:classgraph:4.8.177")
  testCompileOnly(project(":runtime"))
    implementation(kotlin("stdlib-jdk8"))
}

tasks.test {
  useJUnitPlatform()
  val jvmti = project(":jvmti")
  val jdk = project(":instrumentation:jdk")
  val agent = project(":instrumentation:agent")
  executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
  jvmArgs("-agentpath:${jvmti.layout.buildDirectory.get().asFile}/native-libs/libjvmti.so")
  jvmArgs("-javaagent:${agent.layout.buildDirectory.get().asFile}/libs/" +
      "${agent.base.archivesName.get()}-${agent.version}.jar")
  jvmArgs("-Dfray.debug=true")
  dependsOn(":instrumentation:jdk:build")
  dependsOn(":instrumentation:agent:build")
  dependsOn(":jvmti:build")
}
kotlin {
    jvmToolchain(11)
}
