package cmu.pasta.fray.gradle

import cmu.pasta.fray.gradle.tasks.PrepareWorkspaceTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

class FrayPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    val os = DefaultNativePlatform.getCurrentOperatingSystem().toFamilyName()
    val arch = DefaultNativePlatform.getCurrentArchitecture().name
    val frayJdk = target.dependencies.add("testImplementation", "cmu.pasta.fray:jdk:1.0")
    val frayJvmti =
        target.dependencies.add("testImplementation", "cmu.pasta.fray:jvmti:1.0:$os-$arch")
    val frayInstrumentation =
        target.dependencies.add("testImplementation", "cmu.pasta.fray:instrumentation:1.0")
    val javaPath = "${target.rootProject.layout.buildDirectory.get().asFile}/${Commons.JAVA_PATH}"
    val jvmtiPath = "${target.rootProject.layout.buildDirectory.get().asFile}/${Commons.JVMTI_BASE}"
    target.dependencies.add("testImplementation", "cmu.pasta.fray:core:1.0")
    target.dependencies.add("testImplementation", "cmu.pasta.fray:junit:1.0")
    target.dependencies.add("testCompileOnly", "cmu.pasta.fray:runtime:1.0")
    val jlink =
        target.tasks.register("jlink", PrepareWorkspaceTask::class.java).get().apply {
          this.frayJdk.set(frayJdk)
          this.frayJvmti.set(frayJvmti)
        }
    target.tasks.register("frayTest", Test::class.java) {
      it.useJUnitPlatform { it.includeEngines("fray") }
      it.executable(javaPath)
      it.jvmArgs("-agentpath:$jvmtiPath/libjvmti.so")
      it.jvmArgs(
          "-javaagent:${it.project.configurations.detachedConfiguration(frayInstrumentation).resolve().first()}")
      it.jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
      it.jvmArgs("--add-opens", "java.base/java.util.concurrent.atomic=ALL-UNNAMED")
      it.jvmArgs("--add-opens", "java.base/java.util=ALL-UNNAMED")
      it.jvmArgs("--add-opens", "java.base/java.io=ALL-UNNAMED")
      it.jvmArgs("--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED")
      it.jvmArgs("--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED")
      it.dependsOn(jlink)
    }
  }
}
