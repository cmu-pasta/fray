package org.pastalab.fray.plugins.base

import java.nio.file.Path
import kotlin.io.path.div

object Commons {
  private val DIR_BASE = "fray"

  fun getFrayJavaPath(buildDir: Path): Path {
    return getFrayJavaHomePath(buildDir) / "bin" / ("java" + if (isWindows()) ".exe" else "")
  }

  fun getFrayWorkDir(buildDir: Path): Path {
    return buildDir / DIR_BASE
  }

  fun getFrayReportDir(buildDir: Path): Path {
    return buildDir / DIR_BASE / "fray-report"
  }

  fun getFrayJvmtiDirPath(buildDir: Path): Path {
    return buildDir / DIR_BASE / "fray-jvmti"
  }

  fun getFrayJvmtiPath(buildDir: Path): Path {
    return getFrayJvmtiDirPath(buildDir) / ("libjvmti" + if (isWindows()) ".dll" else ".so")
  }

  fun getFrayJavaHomePath(buildDir: Path): Path {
    return buildDir / DIR_BASE / "fray-java"
  }

  fun getFrayVersionPath(buildDir: Path): Path {
    return getFrayJavaHomePath(buildDir) / "fray-version"
  }

  fun isWindows(): Boolean {
    return System.getProperty("os.name").lowercase().contains("windows")
  }
}
