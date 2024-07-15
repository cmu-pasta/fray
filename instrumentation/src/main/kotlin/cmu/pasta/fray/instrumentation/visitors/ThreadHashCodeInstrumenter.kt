package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

class ThreadHashCodeInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  override fun visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    return object :
        AdviceAdapter(
            ASM9,
            super.visitMethod(access, name, descriptor, signature, exceptions),
            access,
            name,
            descriptor) {
      override fun visitMethodInsn(
          opcodeAndSource: Int,
          owner: String,
          name: String,
          descriptor: String,
          isInterface: Boolean
      ) {
        if (name == "hashCode" && owner == "java/lang/Object") {
          invokeStatic(
              Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
              Utils.kFunctionToASMMethod(Runtime::onThreadHashCode),
          )
        } else {
          super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
        }
      }
    }
  }
}
