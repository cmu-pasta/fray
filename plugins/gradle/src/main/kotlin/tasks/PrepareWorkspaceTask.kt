package org.pastalab.fray.gradle.tasks

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.pastalab.fray.gradle.Commons

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
      val correttoJdk = downloadJDK()
      project.exec {
        val jdkJar = project.configurations.detachedConfiguration(frayJdk.get()).resolve().first()
        val dependencies =
            resolveDependencyFiles(frayJdk.get()).map { it.absolutePath }.joinToString(":")
        val command =
            listOf(
                "${correttoJdk}/bin/jlink",
                "-J-javaagent:$jdkJar",
                "-J--module-path=$dependencies",
                "-J--add-modules=org.pastalab.fray.instrumentation.jdk",
                "-J--class-path=$dependencies",
                "--output=$jdkPath",
                "--add-modules=ALL-MODULE-PATH",
                "--fray-instrumentation",
            )
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

  private fun downloadJDK(): String {
    val workDir = "${project.rootProject.layout.buildDirectory.get().asFile}/${Commons.DIR_BASE}"
    val osName = System.getProperty("os.name").lowercase()
    val downloadUrl = getDownloadUrl(osName)
    val fileName = "$workDir/${downloadUrl.substringAfterLast("/")}"
    val jdkFolder = File("$workDir/${getJDKFolderName(osName)}")
    if (jdkFolder.exists()) {
      jdkFolder.deleteRecursively()
    }
    jdkFolder.mkdirs()
    try {
      downloadFile(downloadUrl, fileName)
      decompressFile(fileName, workDir)
    } catch (e: Exception) {
      throw RuntimeException("Download failed: ${e.message}", e)
    }
    return jdkFolder.absolutePath
  }

  fun getDownloadUrl(osName: String): String {
    return when {
      osName.contains("win") ->
          "https://corretto.aws/downloads/resources/21.0.6.7.1/amazon-corretto-21.0.6.7.1-windows-x64-jdk.zip"
      osName.contains("linux") ->
          "https://corretto.aws/downloads/resources/21.0.6.7.1/amazon-corretto-21.0.6.7.1-linux-x64.tar.gz"
      osName.contains("mac") ->
          "https://corretto.aws/downloads/resources/21.0.6.7.1/amazon-corretto-21.0.6.7.1-macosx-aarch64.tar.gz"
      else -> throw RuntimeException("Unsupported OS: $osName")
    }
  }

  fun getJDKFolderName(osName: String): String {
    return when {
      osName.contains("win") -> "jdk21.0.6_7"
      osName.contains("linux") -> "amazon-corretto-21.0.6.7.1-linux-x64"
      osName.contains("mac") -> "amazon-corretto-21.jdk/Contents/Home"
      else -> throw RuntimeException("Unsupported OS: $osName")
    }
  }

  fun downloadFile(fileURL: String, fileName: String) {
    println("Downloading $fileURL to $fileName, subsequent downloads will be skipped.")
    val url = URI.create(fileURL).toURL()
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"

    BufferedInputStream(connection.inputStream).use { input ->
      FileOutputStream(fileName).use { output ->
        val buffer = ByteArray(64 * 1024)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
          output.write(buffer, 0, bytesRead)
        }
      }
    }
  }

  fun decompressFile(fileName: String, outputDir: String) {
    File(outputDir).mkdir()
    when {
      fileName.endsWith(".zip") -> unzipFile(fileName, outputDir)
      fileName.endsWith(".tar.gz") -> untarGzipFile(fileName, outputDir)
      else -> println("Unknown file format, skipping extraction.")
    }
  }

  fun unzipFile(zipFilePath: String, outputDir: String) {
    ZipInputStream(FileInputStream(zipFilePath)).use { zip ->
      var entry = zip.nextEntry
      while (entry != null) {
        val filePath = "$outputDir/${entry.name}"
        if (!entry.isDirectory) {
          FileOutputStream(filePath).use { output -> zip.copyTo(output) }
        } else {
          File(filePath).mkdirs()
        }
        zip.closeEntry()
        entry = zip.nextEntry
      }
    }
  }

  fun untarGzipFile(tarGzFilePath: String, outputDir: String) {
    FileInputStream(tarGzFilePath).use { fis ->
      GzipCompressorInputStream(fis).use { gzipIn ->
        TarArchiveInputStream(gzipIn).use { tarIn ->
          var entry: TarArchiveEntry? = tarIn.nextEntry
          while (entry != null) {
            val outputFile = File(outputDir, entry.name)
            if (entry.isDirectory) {
              outputFile.mkdirs()
            } else {
              outputFile.parentFile.mkdirs()
              FileOutputStream(outputFile).use { output -> tarIn.copyTo(output) }
              outputFile.setExecutable((entry.mode and 0b001_000_000) != 0, false)
              outputFile.setReadable((entry.mode and 0b100_000_000) != 0, false)
              outputFile.setWritable((entry.mode and 0b010_000_000) != 0, false)
            }
            entry = tarIn.nextEntry
          }
        }
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
