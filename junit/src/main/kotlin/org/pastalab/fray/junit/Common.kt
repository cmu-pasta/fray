package org.pastalab.fray.junit

import java.nio.file.Paths

object Common {
  val WORK_DIR = Paths.get(System.getProperty("fray.workDir", "build/fray/fray-report"))
}
