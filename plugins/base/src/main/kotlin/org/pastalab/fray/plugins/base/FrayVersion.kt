package org.pastalab.fray.plugins.base

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Properties

object FrayVersion {
  val version by lazy {
    val properties = Properties()
    javaClass.getResourceAsStream("/org/pastalab/fray/plugins/base/version.properties")?.let {
      properties.load(InputStreamReader(it, StandardCharsets.UTF_8))
    }
    properties.getProperty("version")!!
  }
}
