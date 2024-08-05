package org.pastalab.fray.junit

import java.lang.reflect.Method
import org.junit.platform.engine.TestSource

class MethodSource(private val className: String, private val fieldName: String) : TestSource {
  companion object {
    private const val serialVersionUID = 1L

    fun from(testMethod: Method): MethodSource {
      return MethodSource(testMethod.declaringClass.toGenericString(), testMethod.name)
    }
  }
}
