package cmu.pasta.fray.core.command

import java.net.URI
import java.net.URLClassLoader
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Executor {
  fun execute()
}

@Serializable
@SerialName("executor")
data class MethodExecutor(
    val clazz: String,
    val method: String,
    val args: List<String>,
    val classpaths: List<String>
) : Executor {
  override fun execute() {
    val classLoader =
        URLClassLoader(
            classpaths.map { it -> URI("file://$it").toURL() }.toTypedArray(),
            Thread.currentThread().contextClassLoader)
    Thread.currentThread().contextClassLoader = classLoader
    val clazz = Class.forName(clazz, true, classLoader)
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
  }
}

class LambdaExecutor(val lambda: () -> Unit) : Executor {
  override fun execute() {
    lambda()
  }
}
