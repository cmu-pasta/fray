@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")
package cmu.pasta.sfuzz.jdk.jlink
import jdk.tools.jlink.plugin.Plugin
import jdk.tools.jlink.plugin.ResourcePool
import jdk.tools.jlink.plugin.ResourcePoolBuilder
import jdk.tools.jlink.plugin.ResourcePoolEntry
class JlinkPlugin: Plugin {
    override fun transform(input: ResourcePool, output: ResourcePoolBuilder): ResourcePool {
        input.transformAndCopy({entry ->
            if (entry.type() == ResourcePoolEntry.Type.CLASS_OR_RESOURCE && entry.path().endsWith(".class")) {
                instrumentClass(entry.path(), entry.content())
            }
            entry
        }, output)
        return output.build()
    }
}
