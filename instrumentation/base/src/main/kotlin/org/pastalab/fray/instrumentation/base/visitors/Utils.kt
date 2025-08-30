package org.pastalab.fray.instrumentation.base.visitors

import kotlin.reflect.KFunction
import kotlin.reflect.javaType
import org.objectweb.asm.commons.Method
import org.pastalab.fray.instrumentation.base.RuntimeDescriptors

object Utils {

  @OptIn(ExperimentalStdlibApi::class)
  fun kotlinTypeToJvmDescriptor(kType: kotlin.reflect.KType): String {
    return when (kType.classifier) {
      Int::class -> "I"
      Long::class -> "J"
      Boolean::class -> "Z"
      Byte::class -> "B"
      Char::class -> "C"
      Short::class -> "S"
      Float::class -> "F"
      Double::class -> "D"
      Void::class -> "V"
      Unit::class -> "V"
      IntArray::class -> "[I"
      LongArray::class -> "[J"
      BooleanArray::class -> "[Z"
      ByteArray::class -> "[B"
      CharArray::class -> "[C"
      ShortArray::class -> "[S"
      FloatArray::class -> "[F"
      DoubleArray::class -> "[D"
      else -> {
        val className = kType.javaType.typeName
        "L${className.replace('.', '/')};"
      }
    }
  }

  // Function to convert a KFunction to a JVM method descriptor
  fun kFunctionToJvmMethodDescriptor(function: KFunction<*>): String {
    return RuntimeDescriptors.runtimeMethodMap[function.name] ?: "L$function;"
  }

  fun kFunctionToASMMethod(function: KFunction<*>): Method {
    return Method(function.name, kFunctionToJvmMethodDescriptor(function))
  }
}
