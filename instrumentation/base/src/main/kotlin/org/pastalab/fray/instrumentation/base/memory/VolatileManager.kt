package org.pastalab.fray.instrumentation.base.memory

import java.lang.reflect.Field
import java.lang.reflect.Modifier

class VolatileManager(val tryResolve: Boolean) {
  private val volatileFields = HashMap<String, Boolean>()

  fun setVolatile(name: String, fieldName: String, access: Int) {
    volatileFields["$name:$fieldName"] = access and Modifier.VOLATILE != 0
  }

  fun isVolatile(name: String, fieldName: String): Boolean {
    val key = "$name:$fieldName"
    if (key == "java/util/HashMap:modCount") {
      return true
    } else if (key.startsWith("java/util/HashMap")) {
      return false
    }
    if (key in volatileFields) {
      return volatileFields[key]!!
    }
    if (!tryResolve) {
      return true
    }
    try {
      val cl = Thread.currentThread().getContextClassLoader()
      var clazz = Class.forName(name.replace("/", "."), false, cl)
      var field: Field? = null
      while (clazz != null) {
        try {
          field = clazz.getDeclaredField(fieldName)
          break
        } catch (e: NoSuchFieldException) {
          clazz = clazz.superclass
        }
      }
      if (field != null) {
        volatileFields[key] = field.modifiers and Modifier.VOLATILE != 0
      } else {
        volatileFields[key] = false
      }
    } catch (e: ClassNotFoundException) {
      volatileFields[key] = true
    } catch (e: LinkageError) {
      volatileFields[key] = true
    }
    return volatileFields[key]!!
  }
}
