package org.pastalab.fray.core.command

import java.io.File
import java.lang.reflect.InvocationTargetException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Executor {
  fun execute()

  fun beforeExecution()

  fun afterExecution()
}

@Serializable
@SerialName("executor")
data class MethodExecutor(
    val clazz: String,
    val method: String,
    val args: List<String>,
    val classpaths: List<String>,
    val properties: Map<String, String>
) : Executor {

  override fun beforeExecution() {
    for (property in properties) {
      System.setProperty(property.key, property.value)
    }
  }

  override fun execute() {
    val originalClassLoader = Thread.currentThread().contextClassLoader
    val classLoader =
        FrayClassLoader(
            classpaths.map { File(it).toURI().toURL() }.toTypedArray(), originalClassLoader)
    Thread.currentThread().contextClassLoader = classLoader
    val clazz = Class.forName(clazz, true, Thread.currentThread().contextClassLoader)
    try {
      if (args.isEmpty() && method != "main") {
        val m = clazz.getMethod(method)
        if (m.modifiers and java.lang.reflect.Modifier.STATIC == 0) {
          val obj = clazz.getConstructor().newInstance()
          m.invoke(obj)
        } else {
          m.invoke(null)
        }
      } else {
        val m = clazz.getMethod(method, Array<String>::class.java)
        m.invoke(null, args.toTypedArray())
      }
    } catch (e: InvocationTargetException) {
      throw e.targetException
    } finally {
      Thread.currentThread().contextClassLoader = originalClassLoader
    }
  }

  override fun afterExecution() {}
}

class LambdaExecutor(val lambda: () -> Unit) : Executor {

  override fun beforeExecution() {}

  override fun execute() {
    lambda()
  }

  override fun afterExecution() {}
}
