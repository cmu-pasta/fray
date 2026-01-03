package org.pastalab.fray.instrumentation.base

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import org.pastalab.fray.instrumentation.base.Configs.DEBUG_MODE

object Utils {
  val ORIGIN_CLASS_LOCATION: Path
  val INSTRUMENTED_CLASS_LOCATION: Path

  fun writeClassFile(className: String, classBytes: ByteArray, instrumented: Boolean) {
    if (instrumented) {
      INSTRUMENTED_CLASS_LOCATION.resolve(className.replace("/", ".").removePrefix(".") + ".class")
          .writeBytes(classBytes)
    } else {
      ORIGIN_CLASS_LOCATION.resolve(className.replace("/", ".").removePrefix(".") + ".class")
          .writeBytes(classBytes)
    }
  }

  fun isFrayRuntimeClass(className: String): Boolean {
    val dotClassName = className.replace('/', '.')
    // Check if the class loader is null (bootstrap class loader)
    // and if the class name starts with known JDK prefixes.
    if (
        dotClassName.startsWith("java.") ||
            dotClassName.startsWith("javax.") ||
            dotClassName.startsWith(
                "jdk.",
            ) ||
            dotClassName.startsWith("sun.") ||
            dotClassName.startsWith("com.sun.") ||
            dotClassName.startsWith(
                "kotlin.",
            ) ||
            dotClassName.startsWith("kotlinx.") ||
            (dotClassName.startsWith("org.junit.") &&
                !(dotClassName.contains("ConsoleLauncher") ||
                    //                dotClassName.contains("NamespacedHierarchicalStore") ||
                    dotClassName.contains("LauncherConfigurationParameters"))) ||
            dotClassName.startsWith("org.jetbrains.") ||
            dotClassName.startsWith("com.intellij.rt") ||
            dotClassName.startsWith("worker.org.gradle.") ||
            dotClassName.startsWith(
                "com.github.ajalt",
            ) ||
            (dotClassName.startsWith("org.pastalab.fray") &&
                !dotClassName.startsWith("org.pastalab.fray.example") &&
                !dotClassName.startsWith("org.pastalab.fray.benchmark") &&
                !dotClassName.startsWith("org.pastalab.fray.test") &&
                !dotClassName.startsWith("org.pastalab.fray.junit.internal") &&
                !dotClassName.startsWith(
                    "org.pastalab.fray.core.test",
                ))
    ) {
      return true
    }
    return false
  }

  init {
    val location = Files.createTempDirectory("fray-instrumentation")
    ORIGIN_CLASS_LOCATION = location.resolve("origin")
    ORIGIN_CLASS_LOCATION.createDirectories()
    INSTRUMENTED_CLASS_LOCATION = location.resolve("instrumented")
    INSTRUMENTED_CLASS_LOCATION.createDirectories()
    if (DEBUG_MODE) {
      println("Debug mode enabled, classes will be written to: $location")
    }
  }
}
