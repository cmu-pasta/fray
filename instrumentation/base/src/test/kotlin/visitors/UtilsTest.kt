package visitors

import kotlin.reflect.KFunction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.pastalab.fray.instrumentation.base.visitors.Utils
import org.pastalab.fray.instrumentation.base.visitors.Utils.kotlinTypeToJvmDescriptor

fun exampleMethod(param1: String, param2: Int): Long = 42L

class UtilsTest {

  fun kFunctionToJvmMethodDescriptorThroughReflection(function: KFunction<*>): String {
    val parameterDescriptors =
        function.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .joinToString(separator = "", transform = { kotlinTypeToJvmDescriptor(it.type) })

    val returnDescriptor = kotlinTypeToJvmDescriptor(function.returnType)
    return "($parameterDescriptors)$returnDescriptor"
  }

  @Test
  fun allRuntimeMethodsHaveValidDescriptors() {
    val runtimeClass = org.pastalab.fray.runtime.Runtime::class
    val methods = runtimeClass.members.filter { it.name.startsWith("on") }
    for (method in methods) {
      if (method is KFunction<*>) {
        val descriptor = Utils.kFunctionToJvmMethodDescriptor(method)
        assertEquals(kFunctionToJvmMethodDescriptorThroughReflection(method), descriptor)
      }
    }
  }
}
