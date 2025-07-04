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
      INSTRUMENTED_CLASS_LOCATION.resolve(className.replace("/", ".").removePrefix("."))
          .writeBytes(classBytes)
    } else {
      ORIGIN_CLASS_LOCATION.resolve(className.replace("/", ".").removePrefix("."))
          .writeBytes(classBytes)
    }
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
