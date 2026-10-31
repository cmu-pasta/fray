package org.pastalab.fray.plugins.base

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class FrayWorkspaceInitializerTest {
  @TempDir lateinit var directory: Path

  private fun initializer(): FrayWorkspaceInitializer {
    val jar = directory.resolve("unused.jar").toFile()
    return FrayWorkspaceInitializer(directory, jar, emptySet(), jar)
  }

  @ParameterizedTest
  @CsvSource(
      "amd64,x64",
      "x86_64,x64",
      "x86-64,x64",
      "x8664,x64",
      "x64,x64",
      "aarch64,aarch64",
      "arm64,aarch64",
  )
  fun linuxDownloadMatchesExtractedDirectory(architecture: String, expected: String) {
    val initializer = initializer()
    val folder = "amazon-corretto-${initializer.jdkVersion}-linux-$expected"
    assertEquals(folder, initializer.getJDKFolderName("linux", architecture))
    assertEquals(
        "https://corretto.aws/downloads/resources/${initializer.jdkVersion}/$folder.tar.gz",
        initializer.getDownloadUrl("linux", architecture),
    )
  }

  @Test
  fun unsupportedLinuxArchitectureIsRejected() {
    val initializer = initializer()
    assertThrows(IllegalArgumentException::class.java) {
      initializer.getDownloadUrl("linux", "riscv64")
    }
    assertThrows(IllegalArgumentException::class.java) {
      initializer.getJDKFolderName("linux", "riscv64")
    }
  }
}
