package org.pastalab.fray.maven

import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import org.pastalab.fray.plugins.base.Commons
import org.pastalab.fray.plugins.base.FrayVersion
import org.pastalab.fray.plugins.base.FrayWorkspaceInitializer

@Mojo(
    name = "prepare-fray",
    defaultPhase = LifecyclePhase.INITIALIZE,
    requiresDependencyResolution = ResolutionScope.RUNTIME,
    threadSafe = true)
class PrepareFrayMojo : AbstractMojo() {
  @Parameter(defaultValue = "\${project}", readonly = true, required = true)
  private val project: MavenProject? = null

  @Parameter(property = "plugin.artifactMap", required = true, readonly = true)
  private val pluginArtifactMap: Map<String, Artifact>? = null

  @Parameter(property = "plugin.jdkPath") private val originalJdkPath: String? = null

  @Throws(MojoExecutionException::class)
  override fun execute() {
    val jlinkJar =
        pluginArtifactMap!!["org.pastalab.fray" + ".instrumentation:fray-instrumentation-jdk"]!!
            .file
    val jlinkDependencies =
        pluginArtifactMap.values
            .filter {
              !(it.artifactId == "fray-instrumentation-agent" ||
                  it.artifactId.contains("fray-jvmti"))
            }
            .map { it.file }
    val initializer =
        FrayWorkspaceInitializer(
            Path(project!!.build.directory), jlinkJar, jlinkDependencies.toSet(), getJvmtiJarFile())
    initializer.createInstrumentedJDK(FrayVersion.version, originalJdkPath)
    initializer.createJVMTiRuntime()

    val oldValue = project.properties.getProperty("argLine") ?: ""
    project.properties.setProperty(
        "argLine",
        oldValue +
            " -javaagent:" +
            getAgentJarFile().absolutePath +
            " -agentpath:" +
            Commons.getFrayJvmtiPath(Path(project.build.directory)).absolutePathString() +
            " -Dfray.workDir=" +
            initializer.reportPath)
    project.properties.setProperty(
        "jvm", Commons.getFrayJavaPath(Path(project.build.directory)).absolutePathString())
  }

  fun getAgentJarFile(): File {
    return pluginArtifactMap!!["org.pastalab.fray.instrumentation:fray-instrumentation-agent"]!!
        .file
  }

  fun getJvmtiJarFile(): File {
    val osName = System.getProperty("os.name").lowercase()
    val os =
        if (osName.contains("mac")) "macos"
        else if (osName.contains("linux")) "linux"
        else if (osName.contains("windows")) "windows" else throw Exception("Unsupported OS")

    val arch =
        System.getProperty("os.arch").replace("-", "").let { if (it == "amd64") "x8664" else it }
    return pluginArtifactMap!!["org.pastalab.fray:fray-jvmti-$os-$arch"]!!.file
  }
}
