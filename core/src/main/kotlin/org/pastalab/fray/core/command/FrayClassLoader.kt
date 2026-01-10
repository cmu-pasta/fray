package org.pastalab.fray.core.command

import java.net.URL
import java.net.URLClassLoader
import java.security.CodeSource
import java.security.ProtectionDomain
import org.pastalab.fray.instrumentation.base.Utils.isFrayRuntimeClass
import org.pastalab.fray.runtime.Runtime

class FrayClassLoader(urls: Array<URL>, parent: ClassLoader) : URLClassLoader(urls, parent) {

  override fun loadClass(name: String): Class<*> {
    try {
      Runtime.onSkipPrimitive("FrayClassLoader.loadClass")
      synchronized(getClassLoadingLock(name)) {
        val loaded = findLoadedClass(name)
        if (loaded != null) {
          return loaded
        }
        if (isFrayRuntimeClass(name) || skipFrayClassLoading(name)) {
          return super.loadClass(name)
        }
        try {
          val (classBytes, protectionDomain) = getClassBytesAndProtectionDomain(name)
          return defineClass(name, classBytes, 0, classBytes.size, protectionDomain)
        } catch (exception: Exception) {
          return super.loadClass(name)
        }
      }
    } finally {
      Runtime.onSkipPrimitiveDone("FrayClassLoader.loadClass")
    }
  }

  private fun skipFrayClassLoading(name: String): Boolean {
    // We do not want to reload Mockito classes every time or ByteBuddy classes because
    // they register their own agents and class transformers dynamically, reloading
    // them would register multiple agents and transformers.
    return name.startsWith("org.mockito") || name.startsWith("net.bytebuddy")
  }

  private fun getClassBytesAndProtectionDomain(
      className: String
  ): Pair<ByteArray, ProtectionDomain> {
    val classPath = className.replace('.', '/') + ".class"
    val resource = getResource(classPath) ?: throw ClassNotFoundException(className)
    val codeSource = CodeSource(resource, null as Array<java.security.cert.Certificate>?)
    val protectionDomain = ProtectionDomain(codeSource, null)
    resource.openStream().use { stream ->
      return Pair(stream.readAllBytes(), protectionDomain)
    }
  }
}
