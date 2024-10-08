package org.pastalab.fray.instrumentation.jdk.agent

import java.lang.instrument.Instrumentation
import java.util.*

fun premain(arguments: String?, instrumentation: Instrumentation) {
  val jlinkModule = ModuleLayer.boot().findModule("jdk.jlink").get()
  val fray = ModuleLayer.boot().findModule("org.pastalab.fray.instrumentation.jdk").get()

  val extraExports = mapOf("jdk.tools.jlink.plugin" to Collections.singleton(fray))

  instrumentation.redefineModule(
      jlinkModule, emptySet(), extraExports, emptyMap(), emptySet(), emptyMap())

  val pluginClass = jlinkModule.classLoader.loadClass("jdk.tools.jlink.plugin.Plugin")
  val frayPlugin =
      fray.classLoader.loadClass("org.pastalab.fray.instrumentation.jdk.jlink.JlinkPlugin")

  val extraProvides = mapOf(pluginClass to listOf(frayPlugin))

  instrumentation.redefineModule(
      fray, emptySet(), emptyMap(), emptyMap(), emptySet(), extraProvides)
}
