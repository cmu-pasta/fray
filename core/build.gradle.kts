import java.util.regex.Pattern

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization") version "2.0.0"
}

repositories {
  mavenCentral()
}

dependencies {
  compileOnly(project(":runtime"))
  implementation(project(":rmi"))
  compileOnly(project(":instrumentation:base"))
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
  implementation("com.github.ajalt.clikt:clikt:4.2.2")
  testImplementation("org.jetbrains.kotlin:kotlin-test")
  testCompileOnly(project(":runtime"))
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

tasks.named("build") {
  finalizedBy("genRunner")
  finalizedBy("copyDependencies")
}

tasks.create("genRunner") {
  doLast {
    val instrumentationTask = evaluationDependsOn(":instrumentation:agent")
        .tasks.named("shadowJar").get()
    val instrumentation = instrumentationTask.outputs.files.first().absolutePath
    val core = tasks.named("jar").get().outputs.files.first().absolutePath
    val dependencies = configurations.runtimeClasspath.get().files.joinToString(":") + ":$core"
    val jvmti = project(":jvmti")
    val binDir = "${rootProject.projectDir.absolutePath}/bin"
    var runner = file("${binDir}/fray.template").readText()
    runner = runner.replace("#JVM_TI_PATH#", "${jvmti.layout.buildDirectory.get().asFile}/native-libs/libjvmti.so")
    runner = runner.replace("#AGENT_PATH#", instrumentation)
    runner = runner.replace("#CORE_PATH#", dependencies)
    val file = File("${binDir}/fray")
    file.writeText(runner)
    file.setExecutable(true)
  }
}

tasks.register<Copy>("copyDependencies") {
  from(configurations.runtimeClasspath)
  into("${layout.buildDirectory.get().asFile}/dependency")
}
