import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
  id("java")
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.jmh)
}

group = "org.pastalab.fray.benchmark"

dependencies {
  implementation(project(":core"))
  compileOnly(project(":runtime"))
}

val jvmti = project(":jvmti")
val jdk = project(":instrumentation:jdk")
val agent = project(":instrumentation:agent")
val soSuffix = if (getCurrentOperatingSystem().toFamilyName() == "windows") "dll" else "so"
val javaExec = if (getCurrentOperatingSystem().toFamilyName() == "windows") "java.exe" else "java"

jmh {
  jvm.set(
      "${jdk.layout.buildDirectory.get().asFile}${File.separator}java-inst${File.separator}bin${File.separator}$javaExec"
  )
  jvmArgs.set(
      listOf(
          "-agentpath:${jvmti.layout.buildDirectory.get().asFile}${File.separator}native-libs${File.separator}libjvmti.$soSuffix",
          "-javaagent:${agent.layout.buildDirectory.get().asFile}${File.separator}libs${File.separator}${agent.base.archivesName.get()}-${agent.version}.jar",
      )
  )
  warmupIterations.set(1)
  iterations.set(5)
  fork.set(1)
  resultsFile.set(project.layout.buildDirectory.file("reports/jmh/results.json"))
  resultFormat.set("JSON")
}

tasks.named("jmh") {}
