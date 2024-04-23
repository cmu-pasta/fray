package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
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
              Runtime::class.java.name.replace(".", "/"),
              Runtime::onExit.name,
              Utils.kFunctionToJvmMethodDescriptor(Runtime::onExit),
              false)
        } else {
          super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
      }
    }
  }
}
