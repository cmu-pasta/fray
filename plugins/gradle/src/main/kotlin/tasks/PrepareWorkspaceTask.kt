package org.pastalab.fray.gradle.tasks

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.pastalab.fray.plugins.base.FrayWorkspaceInitializer

abstract class PrepareWorkspaceTask : DefaultTask() {

  @get:Input abstract val frayJdk: Property<Dependency>

  @get:Input abstract val frayJvmti: Property<Dependency>

  @get:Input abstract val frayVersion: Property<String>

  @get:Optional @get:Input abstract val originalJdkPath: Property<String>

  @TaskAction
  fun run() {
    val initializer =
        FrayWorkspaceInitializer(
            project.rootProject.layout.buildDirectory.get().asFile.toPath(),
            project.configurations.detachedConfiguration(frayJdk.get()).resolve().first(),
            resolveDependencyFiles(frayJdk.get()),
            resolveDependencyFiles(frayJvmti.get()).first { it.name.contains("jvmti") },
        )
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
