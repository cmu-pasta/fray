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
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactRequest
import org.pastalab.fray.plugins.base.Commons
import org.pastalab.fray.plugins.base.FrayVersion
import org.pastalab.fray.plugins.base.FrayWorkspaceInitializer

@Mojo(
    name = "prepare-fray",
    defaultPhase = LifecyclePhase.INITIALIZE,
    requiresDependencyResolution = ResolutionScope.RUNTIME,
    threadSafe = true,
)
class PrepareFrayMojo : AbstractMojo() {
  @Parameter(defaultValue = "\${project}", readonly = true, required = true)
  private val project: MavenProject? = null

  @Parameter(property = "plugin.artifactMap", required = true, readonly = true)
  private val pluginArtifactMap: Map<String, Artifact>? = null

  @Parameter(property = "plugin.jdkPath") private val originalJdkPath: String? = null

  @org.apache.maven.plugins.annotations.Component private lateinit var repoSystem: RepositorySystem

  @Parameter(defaultValue = "\${repositorySystemSession}", readonly = true)
  private lateinit var repoSession: RepositorySystemSession

  @Parameter(defaultValue = "\${project.remoteProjectRepositories}", readonly = true)
  private lateinit var remoteRepos: List<RemoteRepository>

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
            Path(project!!.build.directory),
            jlinkJar,
            jlinkDependencies.toSet(),
            getJvmtiJarFile(),
        )
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
            initializer.reportPath,
    )
    project.properties.setProperty(
        "jvm",
        Commons.getFrayJavaPath(Path(project.build.directory)).absolutePathString(),
    )
  }

  fun getAgentJarFile(): File {
    return pluginArtifactMap!!["org.pastalab.fray.instrumentation:fray-instrumentation-agent"]!!
        .file
  }

  fun getJvmtiJarFile(): File {
    val (os, arch) = getOsAndArch()
    val artifactCoords = "org.pastalab.fray:fray-jvmti-$os-$arch:${FrayVersion.version}"
    val artifact = DefaultArtifact(artifactCoords)
    val request = ArtifactRequest(artifact, remoteRepos, null)
    val result = repoSystem.resolveArtifact(repoSession, request)
    return result.artifact.file
  }

  private fun getOsAndArch(): Pair<String, String> {
    val osName = System.getProperty("os.name").lowercase()
    val os =
        if (osName.contains("mac")) "macos"
        else if (osName.contains("linux")) "linux"
        else if (osName.contains("windows")) "windows"
        else throw MojoExecutionException("Unsupported OS")

    val arch =
        System.getProperty("os.arch").replace("-", "").let { if (it == "amd64") "x8664" else it }

    return Pair(os, arch)
  }
}
