package org.pastalab.fray.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junit.JUnitOptions
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.api.tasks.testing.testng.TestNGOptions
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.pastalab.fray.gradle.tasks.FrayTestTask
import org.pastalab.fray.gradle.tasks.PrepareWorkspaceTask
import org.pastalab.fray.plugins.base.Commons

class FrayPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val extension = target.extensions.create("fray", FrayExtension::class.java)
    val jlink = target.tasks.register("jlink", PrepareWorkspaceTask::class.java)

    target.afterEvaluate {
      val targetProject = extension.target?.let { target.project(it) } ?: target
      val baseTest = targetProject.tasks.named(extension.testTask, Test::class.java)
      val testTask =
          if (baseTest.name == "test") {
            target.tasks.register("frayTest", FrayTestTask::class.java)
          } else {
            baseTest
          }
      val frayVersion = extension.version
      val os = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
      val arch = DefaultNativePlatform.getCurrentArchitecture().name.replace("-", "")
      val isSupportedOsArch = supportedOsArchitectures.contains("$os-$arch")

      val frayBuildFolder = target.rootProject.layout.buildDirectory.get().asFile.toPath()
      val frayJdkNotation =
          "org.pastalab.fray.instrumentation:fray-instrumentation-jdk:$frayVersion"
      val frayJvmtiNotation = "org.pastalab.fray:fray-jvmti-$os-$arch:$frayVersion"
      val frayInstrumentationNotation =
          "org.pastalab.fray.instrumentation:fray-instrumentation-agent:$frayVersion"
      addFrayDependencies(
          targetProject,
          baseTest.name,
          frayJdkNotation,
          frayJvmtiNotation,
          frayInstrumentationNotation,
          frayVersion,
          isSupportedOsArch,
      )

      if (!isSupportedOsArch) {
        return@afterEvaluate
      }

      val frayJdkClasspath =
          detachedConfiguration(targetProject, frayJdkNotation, transitive = true)
      val frayJdkJar = detachedConfiguration(targetProject, frayJdkNotation, transitive = false)
      val frayJvmtiArchive =
          detachedConfiguration(targetProject, frayJvmtiNotation, transitive = false)
      val frayInstrumentationJar =
          detachedConfiguration(targetProject, frayInstrumentationNotation, transitive = false)
      val javaPath = Commons.getFrayJavaPath(frayBuildFolder).toString()
      val jvmtiPath = Commons.getFrayJvmtiPath(frayBuildFolder).toString()
      val frayWorkDir = Commons.getFrayReportDir(frayBuildFolder).toString()
      val frayDebugger = target.findProperty("fray.debugger")?.toString()
      val frayInstrumentationPath = frayInstrumentationJar.singleFile.absolutePath
      jlink.configure {
        it.frayJdkClasspath.from(frayJdkClasspath)
        it.frayJdkJar.from(frayJdkJar)
        it.frayJvmtiArchive.from(frayJvmtiArchive)
        it.frayVersion.set(frayVersion)
        it.workspaceRoot.set(target.rootProject.layout.buildDirectory)
        it.frayJavaHome.set(target.rootProject.layout.buildDirectory.dir("fray/fray-java"))
        it.frayJvmtiDir.set(target.rootProject.layout.buildDirectory.dir("fray/fray-jvmti"))
        extension.jdkPath?.let(it.originalJdkPath::set)
      }
      configureFrayTask(
          testTask,
          baseTest.get(),
          javaPath,
          jvmtiPath,
          frayInstrumentationPath,
          frayWorkDir,
          frayDebugger,
          jlink,
          copyInputs = true,
      )
    }
  }

  private fun addFrayDependencies(
      project: Project,
      testTaskName: String,
      frayJdkNotation: String,
      frayJvmtiNotation: String,
      frayInstrumentationNotation: String,
      frayVersion: String,
      isSupportedOsArch: Boolean,
  ) {
    addDependencyIfPresent(project, "testCompileOnly", frayJdkNotation)
    if (isSupportedOsArch) {
      addDependencyIfPresent(project, "testImplementation", frayJvmtiNotation)
    } else {
      project.logger.warn(
          "You are building on an unsupported OS/architecture combination; Fray JVMTI agent will not be added as a dependency."
      )
    }
    addDependencyIfPresent(project, "testImplementation", frayInstrumentationNotation)
    addDependencyIfPresent(
        project,
        "testImplementation",
        "org.pastalab.fray:fray-core:$frayVersion",
    )
    addDependencyIfPresent(
        project,
        "testImplementation",
        "org.pastalab.fray:fray-junit:$frayVersion",
    )
    addDependencyIfPresent(
        project,
        "testImplementation",
        "org.pastalab.fray:fray-runtime:$frayVersion",
    )

    if (testTaskName == "test") {
      return
    }

    val configPrefix = testTaskName.replaceFirstChar { it.lowercase() }
    addDependencyIfPresent(project, "${configPrefix}CompileOnly", frayJdkNotation)
    if (isSupportedOsArch) {
      addDependencyIfPresent(project, "${configPrefix}Implementation", frayJvmtiNotation)
    }
    addDependencyIfPresent(project, "${configPrefix}Implementation", frayInstrumentationNotation)
    addDependencyIfPresent(
        project,
        "${configPrefix}Implementation",
        "org.pastalab.fray:fray-core:$frayVersion",
    )
    addDependencyIfPresent(
        project,
        "${configPrefix}Implementation",
        "org.pastalab.fray:fray-junit:$frayVersion",
    )
    addDependencyIfPresent(
        project,
        "${configPrefix}Implementation",
        "org.pastalab.fray:fray-runtime:$frayVersion",
    )
  }

  private fun addDependencyIfPresent(
      project: Project,
      configurationName: String,
      notation: String,
  ) {
    if (project.configurations.findByName(configurationName) != null) {
      project.dependencies.add(configurationName, notation)
    } else {
      project.logger.warn(
          "Fray: expected configuration '{}' not found; dependency '{}' was not added. " +
              "This may indicate a misconfigured test task or source set.",
          configurationName,
          notation,
      )
    }
  }

  private fun configureFrayTask(
      testTaskProvider: TaskProvider<out Test>,
      baseTest: Test,
      javaPath: String,
      jvmtiPath: String,
      frayInstrumentationPath: String,
      frayWorkDir: String,
      frayDebugger: String?,
      jlink: TaskProvider<PrepareWorkspaceTask>,
      copyInputs: Boolean,
  ) {
    testTaskProvider.configure { frayTestTask ->
      frayTestTask.executable(javaPath)
      frayTestTask.javaLauncher.unset()
      if (copyInputs) {
        // Reuse the selected source task's classes and classpath when creating a dedicated frayTest
        // task.
        frayTestTask.testClassesDirs = baseTest.testClassesDirs
        frayTestTask.classpath = baseTest.classpath
      }
      val testFramework = baseTest.testFramework.options
      when (testFramework) {
        is JUnitPlatformOptions ->
            frayTestTask.useJUnitPlatform { options ->
              options.includeTags("FrayTest")
              options.excludeTags(*testFramework.excludeTags.toTypedArray())
              options.includeEngines(*testFramework.includeEngines.toTypedArray())
              options.excludeEngines(*testFramework.excludeEngines.toTypedArray())
            }
        is JUnitOptions -> frayTestTask.useJUnit()
        is TestNGOptions -> frayTestTask.useTestNG()
        else -> throw IllegalArgumentException("Unsupported test framework $testFramework")
      }
      frayTestTask.doFirst {
        frayTestTask.jvmArgs("-agentpath:$jvmtiPath")
        frayTestTask.jvmArgs("-javaagent:$frayInstrumentationPath")
      }
      frayTestTask.jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
      frayTestTask.jvmArgs("--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED")
      frayTestTask.jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
      frayTestTask.jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
      frayTestTask.jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
      frayTestTask.jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED")
      frayTestTask.systemProperty("fray.organize.by.test", "true")
      frayTestTask.jvmArgs("-Dfray.workDir=$frayWorkDir")
      frayDebugger?.let { frayTestTask.jvmArgs("-Dfray.debugger=$it") }
      frayTestTask.dependsOn(jlink)
      for (taskDependency in baseTest.dependsOn) {
        frayTestTask.dependsOn(taskDependency)
      }
    }
  }

  private fun detachedConfiguration(
      target: Project,
      notation: String,
      transitive: Boolean,
  ): Configuration {
    return target.configurations.detachedConfiguration(target.dependencies.create(notation)).apply {
      isTransitive = transitive
    }
  }

  companion object {
    val supportedOsArchitectures = listOf("linux-x8664", "windows-x8664", "macos-aarch64")
  }
}
