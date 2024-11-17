package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class ObjectInstrumenter(cv: ClassVisitor) : ClassVisitorBase(cv, Object::class.java.name) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "wait" && descriptor == "(J)V") {
      return object : AdviceAdapter(Opcodes.ASM9, mv, access, name, descriptor) {
        override fun visitMethodInsn(
            opcodeAndSource: Int,
            owner: String,
            name: String,
            descriptor: String,
            isInterface: Boolean
        ) {
          if (name == "wait0") {
            super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
                org.pastalab.fray.runtime.Runtime::OBJECTWAIT.name,
                Utils.kFunctionToJvmMethodDescriptor(org.pastalab.fray.runtime.Runtime::OBJECTWAIT),
                false)
          } else {
            super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
          }
        }
      }
    }
    return mv
  }
}
