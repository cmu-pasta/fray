package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.pastalab.fray.runtime.Runtime

class SleepInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  override fun visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    return object :
        GeneratorAdapter(
            ASM9,
            super.visitMethod(access, name, descriptor, signature, exceptions),
            access,
            name,
            descriptor) {
      override fun visitMethodInsn(
          opcode: Int,
          owner: String,
          name: String,
          descriptor: String,
          isInterface: Boolean
      ) {
        if (owner == "java/lang/Thread" && name == "sleep") {
          if (descriptor == "(J)V") {
            invokeStatic(
                Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
                Utils.kFunctionToASMMethod(Runtime::onThreadSleepMillis))
          } else if (descriptor == "(JI)V") {
            invokeStatic(
                Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
                Utils.kFunctionToASMMethod(Runtime::onThreadSleepMillisNanos))
          } else {
            invokeStatic(
                Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
                Utils.kFunctionToASMMethod(Runtime::onThreadSleepDuration))
          }
        } else {
          super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
      }
    }
  }
}
