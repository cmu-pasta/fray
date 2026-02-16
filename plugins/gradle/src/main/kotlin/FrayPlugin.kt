package org.pastalab.fray.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
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
      val frayJdk =
          target.dependencies.add(
              "testCompileOnly",
              "org.pastalab.fray.instrumentation:fray-instrumentation-jdk:$frayVersion",
          )
      val frayJvmti =
          target.dependencies.add(
              "testImplementation",
              "org.pastalab.fray:fray-jvmti-$os-$arch:$frayVersion",
          )
      val frayInstrumentation =
          target.dependencies.add(
              "testImplementation",
              "org.pastalab.fray.instrumentation:fray-instrumentation-agent:$frayVersion",
          )!!
      val javaPath = Commons.getFrayJavaPath(frayBuildFolder).toString()
      val jvmtiPath = Commons.getFrayJvmtiPath(frayBuildFolder).toString()
      target.dependencies.add("testImplementation", "org.pastalab.fray:fray-core:$frayVersion")
      target.dependencies.add("testImplementation", "org.pastalab.fray:fray-junit:$frayVersion")
      target.dependencies.add("testImplementation", "org.pastalab.fray:fray-runtime:$frayVersion")
      jlink.configure {
        it.frayJdk.set(frayJdk)
        it.frayJvmti.set(frayJvmti)
        it.frayVersion.set(frayVersion)
        extension.jdkPath?.let(it.originalJdkPath::set)
      }
      frayTest.configure { frayTestTask ->
        frayTestTask.executable(javaPath)
        // Use the same classes and classpath as the built-in 'test' task (Gradle 9 requires this)
        val baseTest = target.tasks.named("test", Test::class.java).get()
        frayTestTask.testClassesDirs = baseTest.testClassesDirs
        frayTestTask.classpath = baseTest.classpath
        val testFramework = baseTest.testFramework.options
        when (testFramework) {
          is JUnitPlatformOptions ->
              frayTestTask.useJUnitPlatform { options ->
                options.includeTags(*testFramework.includeTags.toTypedArray())
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
          frayTestTask.jvmArgs(
              "-javaagent:${frayTestTask.project.configurations.detachedConfiguration(frayInstrumentation).resolve().first()}"
          )
          frayTestTask.jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
          frayTestTask.jvmArgs("--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED")
          frayTestTask.jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
          frayTestTask.jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
          frayTestTask.jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
          frayTestTask.jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED")

          // Organize test results by test class and method
          frayTestTask.systemProperty("fray.organize.by.test", "true")

          // Set base directory for fray tests
          frayTestTask.jvmArgs("-Dfray.workDir=${Commons.getFrayReportDir(frayBuildFolder)}")
          if (target.hasProperty("fray.debugger")) {
            frayTestTask.jvmArgs("-Dfray.debugger=${target.property("fray.debugger")}")
          }
        }
        frayTestTask.dependsOn(jlink)
        for (taskDependency in baseTest.dependsOn) {
          frayTestTask.dependsOn(taskDependency)
        }
      }
    }
  }
}
