package cmu.edu.pasta.fray.junit

import org.junit.platform.engine.TestSource
import java.lang.reflect.Method

class MethodSource(private val className: String, private val fieldName: String) : TestSource {
  companion object {
    private const val serialVersionUID = 1L

    fun from(testMethod: Method): MethodSource {
      return MethodSource(testMethod.declaringClass.toGenericString(), testMethod.name)
    }
  }
}
