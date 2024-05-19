import de.undercouch.gradle.tasks.download.Download
plugins {
  java
  id("de.undercouch.download") version "5.6.0"
}

val buildPath = layout.buildDirectory.get().asFile

dependencies {
  implementation(fileTree(mapOf("dir" to "/Users/aoli/repos/dacapobench/benchmarks", "include" to listOf("*.jar"))))
  implementation(project(":core"))
}


repositories {
  mavenCentral()
}


tasks.register<Download>("downloadDacapo") {
  src("https://download.dacapobench.org/chopin/dacapo-23.11-chopin.zip")
  dest(File(buildPath, "libs/dacapo.zip"))
  onlyIfModified(true)
}

tasks.register<Copy>("unzipDacapo") {
  dependsOn("downloadDacapo")
  from(zipTree("${buildPath}/libs/dacapo.zip"))
  into("${buildPath}/libs/unzipped")
}

tasks.withType<JavaExec> {
  val agentPath: String by rootProject.extra
  val jdk = project(":jdk")
  val instrumentation = project(":instrumentation")
  classpath = sourceSets["main"].runtimeClasspath
  executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
  mainClass = "cmu.pasta.fray.core.MainKt"
  jvmArgs("-agentpath:$agentPath")
  jvmArgs("-javaagent:${instrumentation.layout.buildDirectory.get().asFile}/libs/${instrumentation.name}-${instrumentation.version}-all.jar")
  jvmArgs("-ea")
  doFirst {
    // Printing the full command
    println("Executing command: ${executable} ${jvmArgs!!.joinToString(" ")} -cp ${classpath.asPath} ${mainClass.get()} ${args!!.joinToString(" ")}")
  }
}


tasks.register<JavaExec>("run") {
  val appName = properties["appName"] as String? ?: "avrora"
  val extraArgs = when (val extraArgs = properties["extraArgs"]) {
    is String -> extraArgs.split(" ")
    else -> emptyList()
  }
  args = listOf("Harness", "main", "-a",
      "$appName --size small", "-o", "${layout.buildDirectory.get().asFile}/$appName-report", "--logger", "csv", "--iter", "10000", "-s", "90000000") + extraArgs
}

tasks.register<JavaExec>("replay") {
  val appName = properties["appName"] as String? ?: "avrora"
  args = listOf("Harness", "main", "-o", "/tmp/report", "-a", appName, "--scheduler", "replay", "--path", "${layout.buildDirectory.get().asFile}/$appName-report/schedule_0.json")
}

tasks.register<JavaExec>("debug") {
  val appName = properties["appName"] as String? ?: "avrora"
  args = listOf("Harness", "main", "-o", "${layout.buildDirectory.get().asFile}/$appName-report", "-a", appName, "--scheduler", "fifo")
  jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005")
}
