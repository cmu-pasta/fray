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
      val arch = DefaultNativePlatform.getCurrentArchitecture().name.replace("-", "")
      val frayJdk =
          target.dependencies.add(
              "testCompileOnly",
              "org.pastalab.fray.instrumentation:fray-instrumentation-jdk:$frayVersion")
      val frayJvmti =
          target.dependencies.add(
              "testImplementation", "org.pastalab.fray:fray-jvmti-$os-$arch:$frayVersion")
      val frayInstrumentation =
          target.dependencies.add(
              "testImplementation",
              "org.pastalab.fray.instrumentation:fray-instrumentation-agent:$frayVersion")!!
      val javaPath = "${target.rootProject.layout.buildDirectory.get().asFile}/${Commons.JAVA_PATH}"
      val jvmtiPath =
          "${target.rootProject.layout.buildDirectory.get().asFile}/${Commons.JVMTI_BASE}"
      val soSuffix = if (os == "windows") "dll" else "so"
      target.dependencies.add("testImplementation", "org.pastalab.fray:fray-core:$frayVersion")
      target.dependencies.add("testImplementation", "org.pastalab.fray:fray-junit:$frayVersion")
      target.dependencies.add("testImplementation", "org.pastalab.fray:fray-runtime:$frayVersion")
      val jlink =
          target.tasks.register("jlink", PrepareWorkspaceTask::class.java).get().apply {
            this.frayJdk.set(frayJdk)
            this.frayJvmti.set(frayJvmti)
            this.frayVersion.set(frayVersion)
            if (extension.jdkPath != null) {
              this.originalJdkPath.set(extension.jdkPath!!)
            }
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
        it.jvmArgs("-agentpath:$jvmtiPath/libjvmti.$soSuffix")
        it.jvmArgs(
            "-javaagent:${it.project.configurations.detachedConfiguration(frayInstrumentation).resolve().first()}")
        it.jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
        it.jvmArgs("--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED")
        it.jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
        it.jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
        it.jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
        it.jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED")

        // Organize test results by test class and method
        it.systemProperty("fray.organize.by.test", "true")

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
