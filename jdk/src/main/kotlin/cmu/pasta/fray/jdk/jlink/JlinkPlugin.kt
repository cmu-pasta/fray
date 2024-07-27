@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package cmu.pasta.fray.jdk.jlink

import cmu.pasta.fray.instrumentation.Utils
import cmu.pasta.fray.instrumentation.instrumentClass
import cmu.pasta.fray.instrumentation.instrumentModuleInfo
import cmu.pasta.fray.runtime.Runtime
import java.io.File
import java.util.zip.ZipFile
import jdk.tools.jlink.plugin.Plugin
import jdk.tools.jlink.plugin.ResourcePool
import jdk.tools.jlink.plugin.ResourcePoolBuilder
import jdk.tools.jlink.plugin.ResourcePoolEntry

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

    Utils.prepareDebugFolder("jdk")
    Utils.prepareDebugFolder("origin")
    input.transformAndCopy(
        { entry ->
          var resourcePoolEntry = entry
          if (resourcePoolEntry.type() == ResourcePoolEntry.Type.CLASS_OR_RESOURCE &&
              resourcePoolEntry.path().endsWith("" + ".class")) {
            if (resourcePoolEntry.path().startsWith("/java.base") &&
                resourcePoolEntry.path().endsWith("module-info.class")) {
              // We need to add runtime.jar to JDK
              var runtime =
                  ZipFile(File(Runtime::class.java.protectionDomain.codeSource.location.toURI()))
              var packages = HashSet<String>()
              for (re in runtime.entries()) {
                if (re.name.contains("module-info.class") || !re.name.endsWith(".class")) continue
                File("/tmp/out/${re.name.replace("/", ".").removePrefix(".")}")
                    .writeBytes(runtime.getInputStream(re).readAllBytes())
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
