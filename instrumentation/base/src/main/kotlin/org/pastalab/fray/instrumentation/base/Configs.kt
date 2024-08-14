package org.pastalab.fray.instrumentation.base

object Configs {
  val DEBUG_MODE = System.getProperty("fray.debug", "false").toBoolean()
}
