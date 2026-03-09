package org.pastalab.fray.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.pastalab.fray.plugins.base.FrayWorkspaceInitializer

abstract class PrepareWorkspaceTask : DefaultTask() {

  @get:Classpath abstract val frayJdkClasspath: ConfigurableFileCollection

  @get:Classpath abstract val frayJdkJar: ConfigurableFileCollection

  @get:InputFiles abstract val frayJvmtiArchive: ConfigurableFileCollection

  @get:Input abstract val frayVersion: Property<String>

  @get:Optional @get:Input abstract val originalJdkPath: Property<String>

  @get:Internal abstract val workspaceRoot: DirectoryProperty

  @get:OutputDirectory abstract val frayJavaHome: DirectoryProperty

  @get:OutputDirectory abstract val frayJvmtiDir: DirectoryProperty

  @TaskAction
  fun run() {
    val initializer =
        FrayWorkspaceInitializer(
            workspaceRoot.get().asFile.toPath(),
            frayJdkJar.singleFile,
            frayJdkClasspath.files,
            frayJvmtiArchive.singleFile,
        )
    initializer.createInstrumentedJDK(frayVersion.get(), originalJdkPath.orNull)
    initializer.createJVMTiRuntime()
  }
}
