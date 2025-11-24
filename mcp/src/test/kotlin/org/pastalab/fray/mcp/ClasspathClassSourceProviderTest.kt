package org.pastalab.fray.mcp

import java.net.URLClassLoader
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.div
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

@OptIn(ExperimentalPathApi::class)
class ClasspathClassSourceProviderTest {

  @TempDir lateinit var tmpDir: Path

  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      if (OS.WINDOWS.isCurrentOs) {
        URLConnection.setDefaultUseCaches("file", false)
        URLConnection.setDefaultUseCaches("jar", false)
        URLConnection.setDefaultUseCaches("zip", false)
      }
    }
  }

  @Test
  fun `resolves source from local project structure`() {
    val moduleRoot = tmpDir / "local"
    val sourceFile = moduleRoot / "src" / "main" / "java" / "com" / "example" / "Sample.java"
    Files.createDirectories(sourceFile.parent)
    val sourceContent = "package com.example;\nclass Sample {}\n"
    sourceFile.writeText(sourceContent)

    val classesDir = moduleRoot / "build" / "classes" / "java" / "main"
    val classFile = classesDir / "com" / "example" / "Sample.class"
    writeClassFile(classFile, "com/example/Sample", "Sample.java")

    URLClassLoader(arrayOf(classesDir.toUri().toURL()), null).use { loader ->
      val provider = ClasspathClassSourceProvider(loader, listOf(moduleRoot))
      val source = provider.getClassSource("com.example.Sample")
      assertEquals(sourceContent, source)
    }
  }

  @Test
  fun `resolves source from dependency source jar`() {
    val repoDir = tmpDir / "repo"
    Files.createDirectories(repoDir)
    val classesJar = repoDir / "lib-1.0.jar"
    val sourcesJar = repoDir / "lib-1.0-sources.jar"

    val classBytes = createClassBytes("com/example/lib/LibClass", "LibClass.java")
    JarOutputStream(Files.newOutputStream(classesJar)).use { jar ->
      jar.putNextEntry(JarEntry("com/example/lib/LibClass.class"))
      jar.write(classBytes)
      jar.closeEntry()
    }

    val sourceContent = "package com.example.lib;\nclass LibClass {}\n"
    JarOutputStream(Files.newOutputStream(sourcesJar)).use { jar ->
      jar.putNextEntry(JarEntry("com/example/lib/LibClass.java"))
      jar.write(sourceContent.toByteArray())
      jar.closeEntry()
    }

    URLClassLoader(arrayOf(classesJar.toUri().toURL()), null).use { loader ->
      val provider = ClasspathClassSourceProvider(loader, listOf(repoDir))
      val source = provider.getClassSource("com.example.lib.LibClass")
      assertEquals(sourceContent, source)
    }
  }

  private fun writeClassFile(path: Path, internalName: String, sourceFile: String) {
    val bytes = createClassBytes(internalName, sourceFile)
    Files.createDirectories(path.parent)
    Files.write(path, bytes)
  }

  private fun createClassBytes(internalName: String, sourceFile: String): ByteArray {
    val cw = ClassWriter(0)
    cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC, internalName, null, "java/lang/Object", null)
    cw.visitSource(sourceFile, null)
    val constructor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
    constructor.visitVarInsn(Opcodes.ALOAD, 0)
    constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
    constructor.visitInsn(Opcodes.RETURN)
    constructor.visitMaxs(1, 1)
    constructor.visitEnd()
    cw.visitEnd()
    return cw.toByteArray()
  }
}
