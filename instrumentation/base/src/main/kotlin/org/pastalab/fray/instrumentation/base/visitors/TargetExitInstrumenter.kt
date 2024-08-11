package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9

class TargetExitInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  override fun visitMethod(
      access: Int,
      name: String?,
      descriptor: String?,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    var mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    return object : MethodVisitor(ASM9, mv) {
      override fun visitMethodInsn(
          opcode: Int,
          owner: String,
          name: String,
          descriptor: String?,
          isInterface: Boolean
      ) {
        if (owner == System::class.java.name.replace(".", "/") && name == "exit") {
          super.visitMethodInsn(
              Opcodes.INVOKESTATIC,
              org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
              org.pastalab.fray.runtime.Runtime::onExit.name,
              Utils.kFunctionToJvmMethodDescriptor(org.pastalab.fray.runtime.Runtime::onExit),
              false)
        } else {
          super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
      }
    }
  }
}
