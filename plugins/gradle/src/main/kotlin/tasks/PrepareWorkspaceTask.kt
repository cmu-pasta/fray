package org.pastalab.fray.gradle.tasks

import java.io.File
import kotlin.io.path.Path
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.pastalab.fray.gradle.Commons
import org.pastalab.fray.plugins.base.FrayWorkspaceInitializer

abstract class PrepareWorkspaceTask : DefaultTask() {

  @get:Input abstract val frayJdk: Property<Dependency>

  @get:Input abstract val frayJvmti: Property<Dependency>

  @get:Input abstract val frayVersion: Property<String>

  @get:Optional @get:Input abstract val originalJdkPath: Property<String>

  @Internal
  val jdkPath =
      File("${project.rootProject.layout.buildDirectory.get().asFile}/${Commons.JDK_BASE}")

  @Internal val jdkVersionPath = Path("${jdkPath}/fray-version")

  @Internal
  val jvmtiPath =
      File("${project.rootProject.layout.buildDirectory.get().asFile}/${Commons.JVMTI_BASE}")

  @TaskAction
  fun run() {
    val initializer =
        FrayWorkspaceInitializer(
            jdkPath,
            project.configurations.detachedConfiguration(frayJdk.get()).resolve().first(),
            resolveDependencyFiles(frayJdk.get()),
            jvmtiPath,
            resolveDependencyFiles(frayJvmti.get()).first { it.name.contains("jvmti") },
            "${project.rootProject.layout.buildDirectory.get().asFile}/${Commons.DIR_BASE}")
    initializer.createInstrumentedJDK(frayVersion.get(), originalJdkPath.orNull)
    initializer.createJVMTiRuntime()
  }

  private fun resolveDependencyFiles(dependency: Dependency): Set<File> {
    return project.configurations
        .detachedConfiguration(dependency)
        .apply { isTransitive = true }
        .resolve()
  }
}
