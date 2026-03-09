package org.pastalab.fray.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junit.JUnitOptions
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.api.tasks.testing.testng.TestNGOptions
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.pastalab.fray.gradle.tasks.PrepareWorkspaceTask
import org.pastalab.fray.plugins.base.Commons

class FrayPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    val extension = target.extensions.create("fray", FrayExtension::class.java)
    val jlink = target.tasks.register("jlink", PrepareWorkspaceTask::class.java)
    val frayTest = target.tasks.register("frayTest", Test::class.java)

    target.afterEvaluate {
      val frayVersion = extension.version
      val os = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
      val arch = DefaultNativePlatform.getCurrentArchitecture().name.replace("-", "")
      val frayBuildFolder = target.rootProject.layout.buildDirectory.get().asFile.toPath()
      val frayJdkNotation =
          "org.pastalab.fray.instrumentation:fray-instrumentation-jdk:$frayVersion"
      val frayJvmtiNotation = "org.pastalab.fray:fray-jvmti-$os-$arch:$frayVersion"
      val frayInstrumentationNotation =
          "org.pastalab.fray.instrumentation:fray-instrumentation-agent:$frayVersion"
      target.dependencies.add("testCompileOnly", frayJdkNotation)
      target.dependencies.add("testImplementation", frayJvmtiNotation)
      target.dependencies.add("testImplementation", frayInstrumentationNotation)
      val frayJdkClasspath = detachedConfiguration(target, frayJdkNotation, transitive = true)
      val frayJdkJar = detachedConfiguration(target, frayJdkNotation, transitive = false)
      val frayJvmtiArchive = detachedConfiguration(target, frayJvmtiNotation, transitive = false)
      val frayInstrumentationJar =
          detachedConfiguration(target, frayInstrumentationNotation, transitive = false)
      val javaPath = Commons.getFrayJavaPath(frayBuildFolder).toString()
      val jvmtiPath = Commons.getFrayJvmtiPath(frayBuildFolder).toString()
      val frayWorkDir = Commons.getFrayReportDir(frayBuildFolder).toString()
      val frayDebugger = target.findProperty("fray.debugger")?.toString()
      val frayInstrumentationPath = frayInstrumentationJar.singleFile.absolutePath
      target.dependencies.add("testImplementation", "org.pastalab.fray:fray-core:$frayVersion")
      target.dependencies.add("testImplementation", "org.pastalab.fray:fray-junit:$frayVersion")
      target.dependencies.add("testImplementation", "org.pastalab.fray:fray-runtime:$frayVersion")
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
      frayTest.configure { frayTestTask ->
        frayTestTask.executable(javaPath)
        frayTestTask.javaLauncher.unset()
        // Use the same classes and classpath as the built-in 'test' task (Gradle 9 requires this)
        val baseTest = target.tasks.named("test", Test::class.java).get()
        frayTestTask.testClassesDirs = baseTest.testClassesDirs
        frayTestTask.classpath = baseTest.classpath
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
}
