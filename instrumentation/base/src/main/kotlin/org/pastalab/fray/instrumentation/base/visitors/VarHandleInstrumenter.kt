package org.pastalab.fray.instrumentation.base.visitors

import kotlin.reflect.KFunction
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.pastalab.fray.runtime.Runtime

class VarHandleMethodVisitor(
    mv: MethodVisitor,
    access: Int,
    name: String,
    descriptor: String,
    val signatureMatcher: (String, String) -> Boolean,
    val callbackFunction: KFunction<*>,
    val instrumenter: (VarHandleMethodVisitor, String, Int, MutableList<Int>) -> Unit
) : AdviceAdapter(ASM9, mv, access, name, descriptor) {

  override fun visitMethodInsn(
      opcodeAndSource: Int,
      owner: String,
      name: String,
      descriptor: String,
      isInterface: Boolean
  ) {
    if (signatureMatcher(owner, name)) {
      // For var handle operations, we only need the object reference and its offset (which is
      // always a long). However, we need to customize instrumentation for each operation.
      // So the convention is that the customized instrumentation will always put the `offset`
      // value in the `offset` local variable after it finishes.
      val offset: Int by lazy { newLocal(Type.LONG_TYPE) }

      // If you need to pop some stack values before accessing the offset and reference,
      // create locals to store them here. Note, you also need to add offset at a proper position.
      // They will be loaded back in reverse order.
      val poppedValues = mutableListOf<Int>()
      instrumenter(this, descriptor, offset, poppedValues)
      dup()

      loadLocal(offset)

      visitMethodInsn(
          INVOKESTATIC,
          org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
          callbackFunction.name,
          Utils.kFunctionToJvmMethodDescriptor(callbackFunction),
          false)
      for (local in poppedValues.reversed()) {
        loadLocal(local)
      }
    }
    super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
  }
}

class VarHandleInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, *instrumentedClasses.toTypedArray()) {
  companion object {
    val instrumentedClasses: List<String> by lazy {
      val varHandleNames =
          listOf(
              "java.lang.invoke.VarHandleInts",
              "java.lang.invoke.VarHandleLongs",
              "java.lang.invoke.VarHandleFloats",
              "java.lang.invoke.VarHandleDoubles",
              "java.lang.invoke.VarHandleBooleans",
              "java.lang.invoke.VarHandleBytes",
              "java.lang.invoke.VarHandleShorts",
              "java.lang.invoke.VarHandleChars",
              "java.lang.invoke.VarHandleReferences",
          )
      val subClassNames =
          listOf(
              "FieldInstanceReadWrite",
              "FieldInstanceReadOnly",
              "FieldStaticReadWrite",
              "FieldStaticReadOnly",
              "Array")
      val instrumentedClasses = mutableListOf<String>()
      for (varHandleName in varHandleNames) {
        for (subClassName in subClassNames) {
          instrumentedClasses.add("$varHandleName$$subClassName")
        }
      }
      instrumentedClasses
    }
  }

  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "compareAndSet") {
      return VarHandleMethodVisitor(
          mv,
          access,
          name,
          descriptor,
          { owner, methodName ->
            owner == "jdk/internal/misc/Unsafe" && methodName.contains("compareAndSet")
          },
          Runtime::onUnsafeWriteVolatile,
          { mv, descriptor, offset, poppedValues ->
            // Unsafe compareAndSet methods are called with the following arguments:
            // Object o, long offset, Type expected, Type value
            // We want to call Runtime.onUnsafeWriteVolatile(o, offset)
            // so that we need to store the expected and value arguments and load them later
            val type = Type.getArgumentTypes(descriptor)[2]!!

            poppedValues.add(mv.newLocal(type))
            poppedValues.add(mv.newLocal(type))
            poppedValues.add(offset)

            mv.storeLocal(poppedValues[0])
            mv.storeLocal(poppedValues[1])
            mv.storeLocal(offset)
          },
      )
    }
    if (name == "getVolatile" || name == "setVolatile") {
      return VarHandleMethodVisitor(
          mv,
          access,
          name,
          descriptor,
          { owner, methodName ->
            owner == "jdk/internal/misc/Unsafe" &&
                (methodName.startsWith("get") || methodName.startsWith("put")) &&
                methodName.endsWith("Volatile")
          },
          if (name == "getVolatile") Runtime::onUnsafeReadVolatile
          else Runtime::onUnsafeWriteVolatile,
          { mv, descriptor, offset, poppedValues ->
            if (name.startsWith("set")) {
              val type = Type.getArgumentTypes(descriptor)[2]!!
              poppedValues.add(mv.newLocal(type))
            }
            poppedValues.add(offset)

            for (local in poppedValues) {
              mv.storeLocal(local)
            }
          },
      )
    }
    return mv
  }
}
