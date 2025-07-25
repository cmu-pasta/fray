package org.pastalab.fray.instrumentation.base.visitors

import java.lang.reflect.ParameterizedType
import kotlin.reflect.KFunction
import kotlin.reflect.javaType
import org.objectweb.asm.commons.Method

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
        val type = kType.javaType
        val className =
            if (type is ParameterizedType) {
              type.rawType.typeName
            } else {
              type.typeName
            }
        "L${className.replace('.', '/')};"
      }
    }
  }

  // Function to convert a KFunction to a JVM method descriptor
  fun kFunctionToJvmMethodDescriptor(function: KFunction<*>): String {
    val parameterDescriptors =
        function.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .joinToString(separator = "", transform = { kotlinTypeToJvmDescriptor(it.type) })

    val returnDescriptor = kotlinTypeToJvmDescriptor(function.returnType)
    return "($parameterDescriptors)$returnDescriptor"
  }

  fun kFunctionToASMMethod(function: KFunction<*>): Method {
    return Method(function.name, kFunctionToJvmMethodDescriptor(function))
  }
}
