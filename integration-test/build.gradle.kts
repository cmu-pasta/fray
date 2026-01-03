import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem

plugins { id("java") }

group = "org.pastalab.fray.test"

java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }

dependencies {
  testImplementation(project(":core"))
  testImplementation(libs.classgraph)
  testRuntimeOnly(libs.junit.platform.launcher)
  testCompileOnly(project(":runtime"))
  testCompileOnly(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.junit.platform.testkit)
}

tasks.test {
  useJUnitPlatform()
  maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
  val jvmti = project(":jvmti")
  val jdk = project(":instrumentation:jdk")
  val agent = project(":instrumentation:agent")
  val soSuffix = if (getCurrentOperatingSystem().toFamilyName() == "windows") "dll" else "so"
  val javaExec = if (getCurrentOperatingSystem().toFamilyName() == "windows") "java.exe" else "java"
  executable(
      "${jdk.layout.buildDirectory.get().asFile}${File.separator}java-inst${File.separator}bin${File.separator}$javaExec"
  )
  jvmArgs("-ea")
  jvmArgs("-verify")
  jvmArgs(
      "-agentpath:${jvmti.layout.buildDirectory.get().asFile}${File.separator}native-libs${File.separator}libjvmti.$soSuffix"
  )
  jvmArgs(
      "-javaagent:${agent.layout.buildDirectory.get().asFile}${File.separator}libs${File.separator}" +
          "${agent.base.archivesName.get()}-${agent.version}.jar"
  )
  jvmArgs("-Dfray.debug=true")
  dependsOn(":instrumentation:jdk:build")
  dependsOn(":instrumentation:agent:build")
  dependsOn(":jvmti:build")
}

if (getCurrentOperatingSystem().toFamilyName() != "windows") {
  tasks.register(
      "testRunnerScript",
      Action {
        val integrationTestJar = project.tasks.jar.get().archiveFile.get().asFile.absolutePath
        dependsOn(":core:genRunner")
        doLast {
          val process =
              ProcessBuilder(
                      "./fray",
                      "-cp",
                      integrationTestJar,
                      "org.pastalab.fray.test.core.success.threadpool.ScheduledThreadPoolWorkSteal",
                  )
                  .redirectErrorStream(true)
                  .directory(
                      file(
                          "${rootProject.projectDir.absolutePath}${File.separator}bin${File.separator}"
                      )
                  )
                  .start()
          process.waitFor()
          val output = process.inputStream.bufferedReader().readText()
          if (!output.contains("[INFO]: Error: org.pastalab.fray.runtime.DeadlockException")) {
            throw GradleException("Runner script test failed. Output:\n$output")
          }
        }
      },
  )

  tasks.named("check") { dependsOn("testRunnerScript") }
}
