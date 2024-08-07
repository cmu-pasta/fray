package org.pastalab.fray.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.commons.AdviceAdapter
import org.pastalab.fray.runtime.Runtime

class TimeInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  override fun visitMethod(
      access: Int,
      name: String?,
      descriptor: String?,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {

    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    return object : AdviceAdapter(ASM9, mv, access, name, descriptor) {
      override fun visitMethodInsn(
          opcode: Int,
          owner: String?,
          name: String?,
          descriptor: String?,
          isInterface: Boolean
      ) {
        if (owner == "java/lang/System" && name == "nanoTime") {
          visitMethodInsn(
              INVOKESTATIC, Runtime::class.java.name.replace(".", "/"), "onNanoTime", "()J", false)
        } else {
          super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
      }
    }
  }
}
