package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.pastalab.fray.runtime.Runtime

class TimeInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  override fun visitMethod(
      access: Int,
      name: String?,
      descriptor: String?,
      signature: String?,
      exceptions: Array<out String>?,
  ): MethodVisitor {

    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    return object : GeneratorAdapter(ASM9, mv, access, name, descriptor) {
      override fun visitMethodInsn(
          opcode: Int,
          owner: String?,
          name: String?,
          descriptor: String?,
          isInterface: Boolean,
      ) {
        if (owner == "java/lang/System" && name == "nanoTime") {
          invokeStatic(
              Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
              Utils.kFunctionToASMMethod(Runtime::onNanoTime),
          )
        } else if (owner == "java/lang/System" && name == "currentTimeMillis") {
          invokeStatic(
              Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
              Utils.kFunctionToASMMethod(Runtime::onCurrentTimeMillis),
          )
        } else if (owner == "java/time/Instant" && name == "now") {
          invokeStatic(
              Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
              Utils.kFunctionToASMMethod(Runtime::onInstantNow),
          )
        } else {
          super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
      }
    }
  }
}
