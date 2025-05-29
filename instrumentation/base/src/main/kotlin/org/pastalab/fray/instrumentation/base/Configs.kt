package org.pastalab.fray.instrumentation.base

object Configs {
  var DEBUG_MODE = System.getProperty("fray.debug", "true").toBoolean()
}
