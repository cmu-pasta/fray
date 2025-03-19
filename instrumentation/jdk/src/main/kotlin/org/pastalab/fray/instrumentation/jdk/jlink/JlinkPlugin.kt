@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package org.anonlab.fray.instrumentation.jdk.jlink

import java.io.File
import java.util.zip.ZipFile
import jdk.tools.jlink.plugin.Plugin
import jdk.tools.jlink.plugin.ResourcePool
import jdk.tools.jlink.plugin.ResourcePoolBuilder
import jdk.tools.jlink.plugin.ResourcePoolEntry
import org.anonlab.fray.instrumentation.base.Configs.DEBUG_MODE
import org.anonlab.fray.instrumentation.base.Utils.writeClassFile
import org.anonlab.fray.instrumentation.base.instrumentClass
import org.anonlab.fray.instrumentation.base.instrumentModuleInfo

class JlinkPlugin : Plugin {
  override fun getName(): String {
    return "fray-instrumentation"
  }

  override fun getDescription(): String {
    return "Fray JDK instrumentator."
  }

  override fun getType(): Plugin.Category {
    return Plugin.Category.TRANSFORMER
  }

  override fun transform(input: ResourcePool, output: ResourcePoolBuilder): ResourcePool {
    println("Start fray plugin!")
    input.transformAndCopy(
        { entry ->
          var resourcePoolEntry = entry
          if (resourcePoolEntry.type() == ResourcePoolEntry.Type.CLASS_OR_RESOURCE &&
              resourcePoolEntry.path().endsWith("" + ".class")) {
            if (resourcePoolEntry.path().startsWith("/java.base") &&
                resourcePoolEntry.path().endsWith("module-info.class")) {
              // We need to add runtime.jar to JDK
              var runtime =
                  ZipFile(
                      File(
                          org.anonlab.fray.runtime.Runtime::class
                              .java
                              .protectionDomain
                              .codeSource
                              .location
                              .toURI()))
              var packages = HashSet<String>()
              for (re in runtime.entries()) {
                if (re.name.contains("module-info.class") || !re.name.endsWith(".class")) continue
                if (DEBUG_MODE) {
                  writeClassFile(re.name, runtime.getInputStream(re).readAllBytes(), false)
                }
                output.add(
                    ResourcePoolEntry.create(
                        "/java.base/" + re.name, runtime.getInputStream(re).readAllBytes()))
                packages.add(re.name.substring(0, re.name.lastIndexOf('/')))
              }
              runtime.close()
              resourcePoolEntry =
                  resourcePoolEntry.copyWithContent(
                      instrumentModuleInfo(resourcePoolEntry.content(), packages.toList()))
            } else {
              resourcePoolEntry =
                  resourcePoolEntry.copyWithContent(instrumentClass(entry.path(), entry.content()))
            }
          }
          resourcePoolEntry
        },
        output)
    return output.build()
  }
}
