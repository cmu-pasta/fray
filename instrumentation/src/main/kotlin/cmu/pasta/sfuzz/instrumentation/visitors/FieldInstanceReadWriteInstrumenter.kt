package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

class FieldInstanceReadWriteInstrumenter(cv: ClassVisitor): ClassVisitorBase(cv,
    "java.lang.invoke.VarHandleInts\$FieldInstanceReadWrite",
    "java.lang.invoke.VarHandleLongs\$FieldInstanceReadWrite",
    "java.lang.invoke.VarHandleFloats\$FieldInstanceReadWrite",
    "java.lang.invoke.VarHandleDoubles\$FieldInstanceReadWrite",
    "java.lang.invoke.VarHandleBooleans\$FieldInstanceReadWrite",
    "java.lang.invoke.VarHandleBytes\$FieldInstanceReadWrite",
    "java.lang.invoke.VarHandleShorts\$FieldInstanceReadWrite",
    "java.lang.invoke.VarHandleChars\$FieldInstanceReadWrite",
    "java.lang.invoke.VarHandleReferences\$FieldInstanceReadWrite") {

  override fun instrumentMethod(
    mv: MethodVisitor,
    access: Int,
    name: String,
    descriptor: String,
    signature: String?,
    exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "compareAndSet") {
      val type = Type.getArgumentTypes(descriptor)[2]!!
      return object: AdviceAdapter(ASM9, mv, access, name, descriptor) {
        override fun visitMethodInsn(
          opcodeAndSource: Int,
          owner: String,
          name: String,
          descriptor: String,
          isInterface: Boolean
        ) {
          if (owner == "jdk/internal/misc/Unsafe" && name.contains("compareAndSet")) {
            // Unsafe compareAndSet methods are called with the following arguments:
            // Object o, long offset, Type expected, Type value
            // We want to call Runtime.onUnsafeWriteVolatile(o, offset)
            // so that we need to store the expected and value arguments and load them later
            val expected = newLocal(type)
            val value = newLocal(type)
            val offset = newLocal(Type.LONG_TYPE)

            storeLocal(value)
            storeLocal(expected)
            storeLocal(offset)

            dup()

            loadLocal(offset)

            visitMethodInsn(Opcodes.INVOKESTATIC,
                Runtime::class.java.name.replace(".", "/"),
                Runtime::onUnsafeWriteVolatile.name,
                Utils.kFunctionToJvmMethodDescriptor(Runtime::onUnsafeWriteVolatile),
                false)

            loadLocal(offset)
            loadLocal(expected)
            loadLocal(value)
          }
          super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
        }
      }
    }
    return mv
  }

}
