package org.pastalab.fray.plugins.base

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.*
import java.util.zip.ZipInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

class FrayWorkspaceInitializer(
    val jdkPath: File,
    val jlinkJar: File,
    val jlinkDependencies: Set<File>,
    val jvmtiPath: File,
    val jvmtiJar: File,
    val workDir: String
) {
  val jdkVersionPath = File(jdkPath, "fray-version")

  fun createInstrumentedJDK(frayVersion: String) {
    if (readJDKFrayVersion() != frayVersion) {
      jdkPath.deleteRecursively()
      val jdk = downloadJDK()
      val classPaths = jlinkDependencies.joinToString(":")
      val command =
          arrayOf(
              "$jdk/bin/jlink",
              "-J-javaagent:$jlinkJar",
              "-J--module-path=$classPaths",
              "-J--add-modules=org.pastalab.fray.instrumentation.jdk",
              "-J--class-path=$classPaths",
              "--output=${jdkPath.absolutePath}",
              "--add-modules=ALL-MODULE-PATH",
              "--fray-instrumentation",
          )
      val logFile = File("/tmp/tmp.log")
      val process =
          ProcessBuilder(*command).redirectOutput(logFile).redirectErrorStream(true).start()
      process.waitFor()
      jdkVersionPath.createNewFile()
      jdkVersionPath.writeText(frayVersion)
    }
  }

  fun createJVMTiRuntime() {
    if (!jvmtiPath.exists()) {
      jvmtiPath.mkdirs()
      unzipFile(jvmtiJar.absolutePath, jvmtiPath.absolutePath)
    }
  }

  private fun isRunningOnNixOS(): Boolean {
    return File("/etc/NIXOS").exists()
  }

  private fun downloadJDK(): String {
    val osName = System.getProperty("os.name").lowercase()
    if (osName.contains("linux") && isRunningOnNixOS()) {
      val nixJdkHome = System.getenv("JDK23_HOME")
      println("Running on NixOS, using system JDK: $nixJdkHome")
      if (nixJdkHome != null) {
        return nixJdkHome
      } else {
        throw RuntimeException("Running on NixOS but JDK23_HOME is not set.")
      }
    }
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
          var entry = tarIn.nextEntry
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

  val jdkMajorVersion = "23"
  val jdkMinorVersion = "0"
  val jdkSecurityVersions = arrayOf("2", "7", "1")

  val jdkVersion = "$jdkMajorVersion.$jdkMinorVersion.${jdkSecurityVersions.joinToString(".")}"

  fun getDownloadUrl(osName: String): String {
    return when {
      osName.contains("win") ->
          "https://corretto.aws/downloads/resources/$jdkVersion/amazon-corretto-$jdkVersion-windows-x64-jdk.zip"
      osName.contains("linux") ->
          "https://corretto.aws/downloads/resources/$jdkVersion/amazon-corretto-$jdkVersion-linux-x64.tar.gz"
      osName.contains("mac") ->
          "https://corretto.aws/downloads/resources/$jdkVersion/amazon-corretto-$jdkVersion-macosx-aarch64.tar.gz"
      else -> throw RuntimeException("Unsupported OS: $osName")
    }
  }

  fun getJDKFolderName(osName: String): String {
    return when {
      osName.contains("win") ->
          "jdk$jdkMajorVersion.$jdkMinorVersion.${jdkSecurityVersions[0]}" +
              if (jdkSecurityVersions.size > 1) "_${jdkSecurityVersions[1]}" else ""
      osName.contains("linux") -> "amazon-corretto-$jdkVersion-linux-x64"
      osName.contains("mac") -> "amazon-corretto-$jdkMajorVersion.jdk/Contents/Home"
      else -> throw RuntimeException("Unsupported OS: $osName")
    }
  }

  private fun readJDKFrayVersion(): String {
    return if (jdkVersionPath.exists()) {
      jdkVersionPath.readText()
    } else {
      ""
    }
  }
}
