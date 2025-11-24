package org.pastalab.fray.mcp

import java.io.IOException
import java.net.JarURLConnection
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Enumeration
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile
import kotlin.io.path.readText
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

/**
 * Resolves source files for classes by walking the classpath and filesystem.
 *
 * The provider first locates the `.class` file using the supplied [classLoader]. For classes that
 * are loaded from directories, it walks up to the module root and searches common `src` layouts.
 * For classes loaded from jars, it attempts to open the matching `*-sources.jar` and read the
 * source entry directly from the archive.
 */
class ClasspathClassSourceProvider(
    private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader,
    workspaceRoots: List<Path> = listOf(Paths.get("").toAbsolutePath())
) : ClassSourceProvider {

  private val sourceCache = ConcurrentHashMap<String, String?>()
  private val sourceJarCache = ConcurrentHashMap<Path, Path?>()
  private val workspaceRoots: List<Path> = workspaceRoots.map { it.toAbsolutePath().normalize() }

  override fun getClassSource(className: String): String? {
    val normalized = className.trim()
    if (normalized.isEmpty()) return null
    return sourceCache.computeIfAbsent(normalized) { resolveClassSource(it) }
  }

  private fun resolveClassSource(className: String): String? {
    val resourcePath = className.replace('.', '/') + ".class"
    val resource = classLoader.getResource(resourcePath) ?: return null
    val classBytes = resource.openStream().use { it.readAllBytes() }
    val sourceFileCandidates = sourceFileCandidates(className, classBytes)
    val packageSegments = packageSegments(className)
    val packageResourcePath =
        if (packageSegments.isEmpty()) "" else packageSegments.joinToString(separator = "/")
    val packageFilesystemPath =
        if (packageSegments.isEmpty()) null
        else Paths.get("", *packageSegments.toTypedArray()).normalize()

    return when {
      resource.protocol == "jar" ->
          resolveFromJar(resource, packageResourcePath, sourceFileCandidates)
      resource.protocol == "file" ->
          resolveFromDirectory(resource, packageFilesystemPath, sourceFileCandidates)
      else -> {
        val connection = resource.openConnection()
        if (connection is JarURLConnection) {
          resolveFromJar(connection.jarFileURL, packageResourcePath, sourceFileCandidates)
        } else {
          null
        }
      }
    }
  }

  private fun resolveFromDirectory(
      resource: URL,
      packagePath: Path?,
      sourceFileCandidates: List<String>
  ): String? {
    val classPath =
        try {
          Paths.get(resource.toURI())
        } catch (e: Exception) {
          return null
        }
    var classesRoot = classPath.parent ?: return null
    if (packagePath != null) {
      repeat(packagePath.nameCount) { classesRoot = classesRoot.parent ?: return null }
    }

    val sourceFile =
        findSourceFileFromClasspathRoot(classesRoot, packagePath, sourceFileCandidates)
            ?: searchWorkspace(packagePath, sourceFileCandidates)

    return sourceFile?.let { readSource(it) }
  }

  private fun findSourceFileFromClasspathRoot(
      classesRoot: Path,
      packagePath: Path?,
      sourceFileCandidates: List<String>
  ): Path? {
    var current: Path? = classesRoot
    var depth = 0
    val visited = mutableSetOf<Path>()
    while (current != null && visited.add(current.normalize()) && depth < MAX_PARENT_DEPTH) {
      val srcDir = current.resolve("src")
      findInSrcFolder(srcDir, packagePath, sourceFileCandidates)?.let {
        return it
      }
      current = current.parent
      depth++
    }
    return null
  }

  private fun searchWorkspace(packagePath: Path?, sourceFileCandidates: List<String>): Path? {
    val queue = ArrayDeque<Pair<Path, Int>>()
    val visited = mutableSetOf<Path>()
    workspaceRoots.forEach { queue.add(it to 0) }
    while (queue.isNotEmpty()) {
      val (dir, depth) = queue.removeFirst()
      val normalized = dir.normalize()
      if (!visited.add(normalized)) continue
      findInSrcFolder(dir.resolve("src"), packagePath, sourceFileCandidates)?.let {
        return it
      }
      if (depth >= WORKSPACE_MAX_DEPTH) continue
      for (child in listDirectories(dir)) {
        if (child.fileName.toString() in WORKSPACE_IGNORELIST) continue
        queue.add(child to depth + 1)
      }
    }
    return null
  }

  private fun resolveFromJar(
      resource: URL,
      packageResourcePath: String,
      sourceFileCandidates: List<String>
  ): String? {
    val jarPath = jarPath(resource)?.normalize() ?: return null
    val sourceJar = sourceJarCache.computeIfAbsent(jarPath) { locateSourceJar(it) } ?: return null
    val entryPrefix = if (packageResourcePath.isEmpty()) "" else "$packageResourcePath/"
    return try {
      JarFile(sourceJar.toFile()).use { jar ->
        for (candidate in sourceFileCandidates) {
          val entry = jar.getJarEntry(entryPrefix + candidate) ?: continue
          jar.getInputStream(entry).use {
            return it.bufferedReader().readText()
          }
        }
      }
      null
    } catch (e: IOException) {
      null
    }
  }

  private fun jarPath(resource: URL): Path? {
    return try {
      val connection = resource.openConnection()
      val jarUrl =
          when (connection) {
            is JarURLConnection -> connection.jarFileURL
            else -> {
              val external = resource.toExternalForm()
              val bangIndex = external.indexOf("!")
              if (external.startsWith("jar:") && bangIndex != -1) {
                URL(external.substring(4, bangIndex))
              } else {
                resource
              }
            }
          }
      Paths.get(jarUrl.toURI())
    } catch (e: Exception) {
      null
    }
  }

  private fun locateSourceJar(jarPath: Path): Path? {
    val jarName = jarPath.fileName.toString()
    if (!jarName.endsWith(".jar")) return null
    val candidateName = jarName.removeSuffix(".jar") + "-sources.jar"

    jarPath.parent
        ?.resolve(candidateName)
        ?.takeIf { Files.isRegularFile(it) }
        ?.let {
          return it
        }

    jarPath.parent?.parent?.let { parent ->
      searchForFile(parent, candidateName, depth = 2)?.let {
        return it
      }
    }

    readMavenCoordinates(jarPath)?.let { coordinates ->
      val (group, artifact, version) = coordinates
      val m2Repo = System.getenv("M2_REPO")
      val m2Path =
          if (m2Repo != null && m2Repo.isNotBlank()) {
            Paths.get(m2Repo)
          } else {
            Paths.get(System.getProperty("user.home"), ".m2", "repository")
          }
      val sourceJarPath =
          m2Path
              .resolve(group.replace('.', java.io.File.separatorChar))
              .resolve(artifact)
              .resolve(version)
              .resolve("$artifact-$version-sources.jar")
      if (Files.isRegularFile(sourceJarPath)) {
        return sourceJarPath
      }
    }
    return null
  }

  private fun readMavenCoordinates(jarPath: Path): Triple<String, String, String>? {
    return try {
      JarFile(jarPath.toFile()).use { jar ->
        val entry =
            jar.entries().asSequence().firstOrNull {
              it.name.startsWith("META-INF/maven/") && it.name.endsWith("pom.properties")
            } ?: return null
        jar.getInputStream(entry).use { input ->
          val props = Properties().apply { load(input) }
          val group = props.getProperty("groupId") ?: return null
          val artifact = props.getProperty("artifactId") ?: return null
          val version = props.getProperty("version") ?: return null
          Triple(group, artifact, version)
        }
      }
    } catch (e: IOException) {
      null
    }
  }

  private fun findInSrcFolder(
      srcDir: Path,
      packagePath: Path?,
      sourceFileCandidates: List<String>
  ): Path? {
    if (!Files.isDirectory(srcDir)) return null
    for (child in listDirectories(srcDir)) {
      locateInDirectory(child, packagePath, sourceFileCandidates)?.let {
        return it
      }
      for (grandchild in listDirectories(child)) {
        locateInDirectory(grandchild, packagePath, sourceFileCandidates)?.let {
          return it
        }
      }
    }
    return locateInDirectory(srcDir, packagePath, sourceFileCandidates)
  }

  private fun locateInDirectory(
      base: Path,
      packagePath: Path?,
      sourceFileCandidates: List<String>
  ): Path? {
    val packageRoot = packagePath?.let { base.resolve(it) } ?: base
    for (candidate in sourceFileCandidates) {
      val file = packageRoot.resolve(candidate)
      if (Files.isRegularFile(file)) {
        return file
      }
    }
    return null
  }

  private fun listDirectories(path: Path): List<Path> {
    if (!Files.isDirectory(path)) return emptyList()
    return try {
      Files.newDirectoryStream(path).use { stream ->
        val result = mutableListOf<Path>()
        for (entry in stream) {
          if (Files.isDirectory(entry)) {
            result.add(entry)
          }
        }
        result
      }
    } catch (e: IOException) {
      emptyList()
    }
  }

  private fun packageSegments(className: String): List<String> {
    val dotIndex = className.lastIndexOf('.')
    if (dotIndex == -1) return emptyList()
    return className.substring(0, dotIndex).split('.').filter { it.isNotEmpty() }
  }

  private fun sourceFileCandidates(className: String, classBytes: ByteArray): List<String> {
    val candidates = linkedSetOf<String>()
    readSourceAttribute(classBytes)?.let { candidates.add(it) }
    val leafName = className.substringAfterLast('.').substringBefore('$')
    for (extension in listOf(".java", ".kt", ".kts", ".groovy", ".scala")) {
      candidates.add(leafName + extension)
    }
    return candidates.toList()
  }

  private fun readSourceAttribute(classBytes: ByteArray): String? {
    var sourceFile: String? = null
    val visitor =
        object : ClassVisitor(Opcodes.ASM9) {
          override fun visitSource(source: String?, debug: String?) {
            sourceFile = source
          }
        }
    ClassReader(classBytes).accept(visitor, ClassReader.SKIP_FRAMES)
    return sourceFile
  }

  private fun readSource(path: Path): String? {
    return try {
      path.readText()
    } catch (e: IOException) {
      null
    }
  }

  private fun searchForFile(dir: Path, fileName: String, depth: Int): Path? {
    if (depth < 0 || !Files.isDirectory(dir)) return null
    dir.resolve(fileName)
        .takeIf { Files.isRegularFile(it) }
        ?.let {
          return it
        }
    if (depth == 0) return null
    for (child in listDirectories(dir)) {
      searchForFile(child, fileName, depth - 1)?.let {
        return it
      }
    }
    return null
  }

  companion object {
    private const val MAX_PARENT_DEPTH = 10
    private const val WORKSPACE_MAX_DEPTH = 3
    private val WORKSPACE_IGNORELIST =
        setOf(".gradle", ".git", ".idea", "build", "out", "target", "node_modules")

    fun defaultProvider(): ClasspathClassSourceProvider {
      val loader = Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()
      val roots = inferWorkspaceRoots(loader)
      return ClasspathClassSourceProvider(loader, roots)
    }

    private fun inferWorkspaceRoots(classLoader: ClassLoader): List<Path> {
      if (classLoader !is URLClassLoader) return listOf(Paths.get(""))
      val roots = mutableSetOf<Path>()
      for (url in classLoader.urLs) {
        if (url.protocol != "file") continue
        val path =
            try {
              Paths.get(url.toURI())
            } catch (e: Exception) {
              continue
            }
        var current: Path? = path
        var depth = 0
        while (current != null && depth < MAX_PARENT_DEPTH) {
          if (Files.isDirectory(current.resolve("src"))) {
            roots.add(current)
            break
          }
          current = current.parent
          depth++
        }
      }
      return if (roots.isEmpty()) listOf(Paths.get("")) else roots.toList()
    }
  }
}

private fun <T> Enumeration<T>.asSequence(): Sequence<T> {
  return sequence {
    while (hasMoreElements()) {
      yield(nextElement())
    }
  }
}
