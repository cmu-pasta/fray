package org.pastalab.fray.junit

import java.io.File
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

  fun getPath(resourceLocation: String): File {
    val classPathPrefix = "classpath:"
    return if (resourceLocation.startsWith(classPathPrefix)) {
      val classPathPath = resourceLocation.substring(classPathPrefix.length)
      val classLoader = Thread.currentThread().contextClassLoader
      val url = classLoader.getResource(classPathPath)
      val nonNullUrl = requireNotNull(url) { "Resource '$classPathPath' not found on classpath" }
      File(nonNullUrl.toURI())
    } else {
      File(resourceLocation)
    }
  }
}
