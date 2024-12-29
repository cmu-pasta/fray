package org.pastalab.fray.gradle.tasks

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.pastalab.fray.gradle.Commons
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

abstract class PrepareWorkspaceTask : DefaultTask() {

  @get:Input abstract val frayJdk: Property<Dependency>

  @get:Input abstract val frayJvmti: Property<Dependency>

  @get:Input abstract val frayVersion: Property<String>

  @Internal
  val jdkPath =
      File("${project.rootProject.layout.buildDirectory.get().asFile}/${Commons.JDK_BASE}")

  @Internal val jdkVersionPath = Path("${jdkPath}/fray-version")

  @Internal
  val jvmtiPath =
      File("${project.rootProject.layout.buildDirectory.get().asFile}/${Commons.JVMTI_BASE}")

  @TaskAction
  fun run() {
    if (readJDKFrayVersion() != frayVersion.get()) {
      jdkPath.deleteRecursively()
      project.exec {
        val jdkJar = project.configurations.detachedConfiguration(frayJdk.get()).resolve().first()
        val dependencies =
            resolveDependencyFiles(frayJdk.get()).map { it.absolutePath }.joinToString(":")
        val command =
            listOf(
                "jlink",
                "-J-javaagent:$jdkJar",
                "-J--module-path=$dependencies",
                "-J--add-modules=org.pastalab.fray.instrumentation.jdk",
                "-J--class-path=$dependencies",
                "--output=$jdkPath",
                "--add-modules=ALL-MODULE-PATH",
                "--fray-instrumentation")
        println(command.joinToString(" "))
        it.commandLine(command)
      }
      jdkVersionPath.createFile()
      jdkVersionPath.writeText(frayVersion.get())
    }
    if (!jvmtiPath.exists()) {
      jvmtiPath.mkdirs()
      val jvmtiJar =
          resolveDependencyFiles(frayJvmti.get()).filter { it.name.contains("jvmti") }.first()
      project.copy {
        it.from(project.zipTree(jvmtiJar))
        it.into(jvmtiPath)
      }
    }
  }

  private fun readJDKFrayVersion(): String {
    return if (jdkVersionPath.exists()) {
      jdkVersionPath.readText()
    } else {
      ""
    }
  }

  private fun resolveDependencyFiles(dependency: Dependency): Set<File> {
    return project.configurations
        .detachedConfiguration(dependency)
        .apply { isTransitive = true }
        .resolve()
  }
}
