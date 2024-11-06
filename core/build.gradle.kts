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
      "${agent.name}-${agent.version}-shadow.jar")
  jvmArgs("-Dfray.debug=true")
  dependsOn(":instrumentation:jdk:build")
  dependsOn(":instrumentation:agent:build")
  dependsOn(":jvmti:build")
}

tasks.named("build") {
  finalizedBy("genRunner")
  finalizedBy("copyDependencies")
}

tasks.withType<JavaExec> {
  dependsOn(":instrumentation:jdk:build")
  val instrumentationTask = evaluationDependsOn(":instrumentation:agent")
      .tasks.named("shadowJar").get()
  val jdk = project(":instrumentation:jdk")
  val jvmti = project(":jvmti")
  val instrumentation = instrumentationTask.outputs.files.first().absolutePath
  classpath += tasks.named("jar").get().outputs.files + files(configurations.runtimeClasspath)
  executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
  mainClass = "org.pastalab.fray.core.MainKt"
  jvmArgs("-agentpath:${jvmti.layout.buildDirectory.get().asFile}/native-libs/libjvmti.so")
  jvmArgs("-javaagent:$instrumentation")
  jvmArgs("-ea")
  jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
  jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED")
//  jvmArgs("-Dfray.recordSchedule=true")
  doFirst {
    // Printing the full command
    println("Executing command: ${executable} ${jvmArgs!!.joinToString(" ")} -cp ${classpath.asPath} ${mainClass.get()} ${args!!.joinToString(" ")}")
  }
}

tasks.register<JavaExec>("runFray") {
  var configPath = properties["configPath"] as String? ?: ""
  val extraArgs = when (val extraArgs = properties["extraArgs"]) {
    is String -> {
      val pattern = Pattern.compile("""("[^"]+"|\S+)""")
      val matcher = pattern.matcher(extraArgs)
      val result = mutableListOf<String>()
      while (matcher.find()) {
        result.add(matcher.group(1).replace("\"", ""))
      }
      result
    }
    else -> emptyList()
  }
  if (!File(configPath).isAbsolute) {
    configPath = System.getProperty("user.dir") + "/" + configPath
  }
  args = listOf("--run-config", "json", "--config-path", configPath) + extraArgs
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
