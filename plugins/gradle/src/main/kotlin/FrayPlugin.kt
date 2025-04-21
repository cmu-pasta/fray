package org.pastalab.fray.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junit.JUnitOptions
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.api.tasks.testing.testng.TestNGOptions
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.pastalab.fray.gradle.tasks.PrepareWorkspaceTask

class FrayPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    val extension = target.extensions.create("fray", FrayExtension::class.java)

    target.afterEvaluate {
      val frayVersion = extension.version
      val os = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
      val arch = DefaultNativePlatform.getCurrentArchitecture().name
      val frayJdk =
          target.dependencies.add(
              "testImplementation",
              "org.pastalab.fray.instrumentation:fray-instrumentation-jdk:$frayVersion")
      val frayJvmti =
          target.dependencies.add(
              "testImplementation", "org.pastalab.fray:fray-jvmti-$os-$arch:$frayVersion")
      val frayInstrumentation =
          target.dependencies.add(
              "testImplementation",
              "org.pastalab.fray.instrumentation:fray-instrumentation-agent:$frayVersion")
      val javaPath = "${target.rootProject.layout.buildDirectory.get().asFile}/${Commons.JAVA_PATH}"
      val jvmtiPath =
          "${target.rootProject.layout.buildDirectory.get().asFile}/${Commons.JVMTI_BASE}"
      target.dependencies.add("testImplementation", "org.pastalab.fray:fray-core:$frayVersion")
      target.dependencies.add("testImplementation", "org.pastalab.fray:fray-junit:$frayVersion")
      target.dependencies.add("testCompileOnly", "org.pastalab.fray:fray-runtime:$frayVersion")
      val jlink =
          target.tasks.register("jlink", PrepareWorkspaceTask::class.java).get().apply {
            this.frayJdk.set(frayJdk)
            this.frayJvmti.set(frayJvmti)
            this.frayVersion.set(frayVersion)
          }
      target.tasks.register("frayTest", Test::class.java) {
        it.executable(javaPath)
        val testFramework = target.tasks.named("test", Test::class.java).get().testFramework.options
        when (testFramework) {
          is JUnitPlatformOptions ->
              it.useJUnitPlatform { options ->
                options.includeTags(*testFramework.includeTags.toTypedArray())
                options.excludeTags(*testFramework.excludeTags.toTypedArray())
                options.includeEngines(*testFramework.includeEngines.toTypedArray())
                options.excludeEngines(*testFramework.excludeEngines.toTypedArray())
              }
          is JUnitOptions -> it.useJUnit()
          is TestNGOptions -> it.useTestNG()
          else -> throw IllegalArgumentException("Unsupported test framework $testFramework")
        }
        it.jvmArgs("-agentpath:$jvmtiPath/libjvmti.so")
        it.jvmArgs(
            "-javaagent:${it.project.configurations.detachedConfiguration(frayInstrumentation).resolve().first()}")
        it.jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        it.jvmArgs("--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED")
        it.jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
        it.jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
        it.jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
        it.jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED")

        // Enable test output in Gradle logs
        it.testLogging { it.showStandardStreams = true }

        // Organize test results by test class and method
        it.systemProperty("fray.organize.by.test", "true")

        // Apply configuration from the extension or project properties or system properties

        // Helper function to get property from project property, system property, or extension
        fun <T> getProperty(name: String, extensionValue: T?): Any? {
          return target.findProperty(name)
              ?: // Check project property (-P)
              System.getProperty(name)
              ?: // Check system property (-D)
              extensionValue
        }

        // Record schedule
        val recordSchedule = getProperty("fray.recordSchedule", null)
        if (recordSchedule != null) {
          it.systemProperty("fray.recordSchedule", recordSchedule.toString())
        }

        // Scheduler
        val scheduler = getProperty("fray.scheduler", extension.scheduler)
        if (scheduler != null) {
          it.systemProperty("fray.scheduler", scheduler.toString())
        }

        // Iterations
        val iterations = getProperty("fray.iterations", extension.iterations)
        if (iterations != null) {
          it.systemProperty("fray.iterations", iterations.toString())
        }

        // Replay directory
        val replayDir = getProperty("fray.replayDir", extension.replayDir)
        if (replayDir != null) {
          it.systemProperty("fray.replayDir", replayDir.toString())
        }

        // Timeout
        val timeout = getProperty("fray.timeout", extension.timeout)
        if (timeout != null) {
          it.systemProperty("fray.timeout", timeout.toString())
        }

        // Explore mode
        val exploreMode = getProperty("fray.exploreMode", extension.exploreMode)
        if (exploreMode.toString().toBoolean()) {
          it.systemProperty("fray.exploreMode", "true")
        }

        // Number of switch points (for PCT)
        val numSwitchPoints = getProperty("fray.numSwitchPoints", extension.numSwitchPoints)
        if (numSwitchPoints != null) {
          it.systemProperty("fray.numSwitchPoints", numSwitchPoints.toString())
        }

        // Set base directory for fray tests
        it.jvmArgs(
            "-Dfray.workDir=${target.layout.buildDirectory.get().asFile}/${Commons.TEST_WORK_DIR}")
        if (target.hasProperty("fray.debugger")) {
          it.jvmArgs("-Dfray.debugger=${target.property("fray.debugger")}")
        }
        it.dependsOn(jlink)
      }
    }
  }
}
