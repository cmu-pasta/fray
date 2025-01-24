plugins {
  id("java")
  kotlin("jvm")
  kotlin("plugin.serialization") version "2.0.0"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(project(":core"))
  compileOnly(project(":runtime"))
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
  compileOnly("org.junit.platform:junit-platform-engine:1.11.3")
  compileOnly("org.junit.platform:junit-platform-commons:1.11.3")
  compileOnly("org.junit.jupiter:junit-jupiter-api:5.11.3")
  compileOnly("junit:junit:4.13.2")
  compileOnly("com.carrotsearch.randomizedtesting:randomizedtesting-runner:2.8.2")
  testCompileOnly("org.junit.jupiter:junit-jupiter-api:5.11.3")
  testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.3")
}

tasks.test {
  useJUnitPlatform {
    includeEngines("junit-jupiter", "fray")
  }
  ignoreFailures = true
  dependsOn(":instrumentation:jdk:build")
  val instrumentationTask = evaluationDependsOn(":instrumentation:agent")
      .tasks.named("shadowJar").get()
  val jdk = project(":instrumentation:jdk")
  val jvmti = project(":jvmti")
  val instrumentation = instrumentationTask.outputs.files.first().absolutePath
  classpath += tasks.named("jar").get().outputs.files + files(configurations.runtimeClasspath)
  executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
  jvmArgs("-agentpath:${jvmti.layout.buildDirectory.get().asFile}/native-libs/libjvmti.so")
  jvmArgs("-javaagent:$instrumentation")
  jvmArgs("-ea")
  jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED")
}
