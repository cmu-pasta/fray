package cmu.pasta.sfuzz.jdk.agent

import java.lang.instrument.Instrumentation
import java.util.*
import kotlin.collections.HashMap

fun premain(arguments: String?, instrumentation: Instrumentation) {
    var jlinkModule = ModuleLayer.boot().findModule("jdk.jlink").get()
    var sfuzz = ModuleLayer.boot().findModule("cmu.pasta.sfuzz.jdk").get()

    var extraExports = mapOf("jdk.tools.jlink.plugin" to Collections.singleton(sfuzz))

    instrumentation.redefineModule(
        jlinkModule,
        emptySet(),
        extraExports,
        emptyMap(),
        emptySet(),
        emptyMap()
    )

    var pluginClass = jlinkModule.classLoader.loadClass("jdk.tools.jlink.plugin.Plugin")
    var sfuzzPlugin = sfuzz.classLoader.loadClass("cmu.pasta.sfuzz.jdk.jlink.JlinkPlugin")

    var extraProvides = mapOf(
        pluginClass to listOf(sfuzzPlugin)
    )

    instrumentation.redefineModule(
        sfuzz,
        emptySet(),
        emptyMap(),
        emptyMap(),
        emptySet(),
        extraProvides
    )
}
