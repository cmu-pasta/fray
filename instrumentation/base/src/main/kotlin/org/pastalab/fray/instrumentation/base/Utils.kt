package org.pastalab.fray.instrumentation.base

import java.io.File
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import org.pastalab.fray.instrumentation.base.Configs.DEBUG_MODE

object Utils {
  @OptIn(ExperimentalPathApi::class)
  fun prepareDebugFolder(folder: String) {
    val path = Paths.get("/tmp/out/$folder")
    path.deleteRecursively()
    path.createDirectories()
  }

  fun writeClassFile(className: String, classBytes: ByteArray, instrumented: Boolean) {
    if (instrumented) {
      File("/tmp/out/instrumented/${className.replace("/", ".").removePrefix(".")}.class")
          .writeBytes(classBytes)
    } else {
      File("/tmp/out/origin/${className.replace("/", ".").removePrefix(".")}.class")
          .writeBytes(classBytes)
    }
  }

  init {
    if (DEBUG_MODE) {
      prepareDebugFolder("instrumented")
      prepareDebugFolder("origin")
    }
  }
}
