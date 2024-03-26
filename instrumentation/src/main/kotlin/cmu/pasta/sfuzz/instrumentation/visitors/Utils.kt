package cmu.pasta.sfuzz.instrumentation.visitors

import kotlin.reflect.KFunction
import kotlin.reflect.javaType

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
    val parameterDescriptors =
        function.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .joinToString(separator = "", transform = { kotlinTypeToJvmDescriptor(it.type) })

    val returnDescriptor = kotlinTypeToJvmDescriptor(function.returnType)
    return "($parameterDescriptors)$returnDescriptor"
  }
}
