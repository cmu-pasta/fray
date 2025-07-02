import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins {
  id("java")
}

group = "org.pastalab.fray.test"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(23))
  }
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testImplementation(project(":core", configuration = "shadow"))
  testImplementation("io.github.classgraph:classgraph:4.8.177")
  testCompileOnly(project(":runtime"))
}

tasks.test {
  useJUnitPlatform()
  maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
  val jvmti = project(":jvmti")
  val jdk = project(":instrumentation:jdk")
  val agent = project(":instrumentation:agent")
  val soSuffix = if (getCurrentOperatingSystem().toFamilyName() == "windows") "dll" else "so"
  executable("${jdk.layout.buildDirectory.get().asFile}/java-inst/bin/java")
  jvmArgs("-ea")
  jvmArgs("-verify")
  jvmArgs("-agentpath:${jvmti.layout.buildDirectory.get().asFile}/native-libs/libjvmti.$soSuffix")
  jvmArgs("-javaagent:${agent.layout.buildDirectory.get().asFile}/libs/" +
      "${agent.base.archivesName.get()}-${agent.version}.jar")
  jvmArgs("-Dfray.debug=true")
  dependsOn(":instrumentation:jdk:build")
  dependsOn(":instrumentation:agent:build")
  dependsOn(":jvmti:build")
}

if (getCurrentOperatingSystem().toFamilyName() != "windows") {
  tasks.register("testRunnerScript") {
    val integrationTestJar = project.tasks.jar.get().archiveFile.get().asFile.absolutePath
    dependsOn(":core:genRunner")
    doLast {
      val process = ProcessBuilder(
          "./fray",
          "-cp", integrationTestJar,
          "org.pastalab.fray.test.core.success.threadpool.ScheduledThreadPoolWorkSteal"
      )
          .redirectErrorStream(true)
          .directory(file("${rootProject.projectDir.absolutePath}/bin/"))
          .start()
      process.waitFor()
      val output = process.inputStream.bufferedReader().readText()
      if (!output.contains("[INFO]: Error: org.pastalab.fray.runtime.DeadlockException")) {
        throw GradleException("Runner script test failed. Output:\n$output")
      }
    }
  }

  tasks.named("check") {
    dependsOn("testRunnerScript")
  }
}
