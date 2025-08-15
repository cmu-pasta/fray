package org.pastalab.fray.junit

import java.nio.file.Path
import java.nio.file.Paths

object Common {
  val WORK_DIR: Path = Paths.get(System.getProperty("fray.workDir", "build/fray/fray-report"))

  fun getTestDir(className: String? = null, methodName: String? = null): Path {
    val baseDir = WORK_DIR

    if (System.getProperty("fray.organize.by.test", "false").toBoolean() &&
        className != null &&
        methodName != null) {
      return baseDir.resolve(className).resolve(methodName)
    }

    return baseDir
  }
}
