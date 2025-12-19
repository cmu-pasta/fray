package org.pastalab.fray.core.command

import java.net.URL
import java.net.URLClassLoader
import org.pastalab.fray.instrumentation.base.Utils.isFrayRuntimeClass
import org.pastalab.fray.runtime.Runtime

class FrayClassLoader(urls: Array<URL>, parent: ClassLoader) : URLClassLoader(urls, parent) {

  override fun loadClass(name: String): Class<*> {
    try {
      Runtime.onSkipScheduling("FrayClassLoader.loadClass")
      synchronized(getClassLoadingLock(name)) {
        val loaded = findLoadedClass(name)
        if (loaded != null) {
          return loaded
        }
        if (isFrayRuntimeClass(name)) {
          return super.loadClass(name)
        }
        try {
          val classBytes = getClassBytes(name)
          return defineClass(name, classBytes, 0, classBytes.size)
        } catch (exception: Exception) {
          return super.loadClass(name)
        }
      }
    } finally {
      Runtime.onSkipSchedulingDone("FrayClassLoader.loadClass")
    }
  }

  private fun getClassBytes(className: String): ByteArray {
    val classPath = className.replace('.', '/') + ".class"
    getResourceAsStream(classPath).use { stream ->
      if (stream == null) {
        throw ClassNotFoundException(className)
      }
      return stream.readAllBytes()
    }
  }
}
