package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

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
          invokeStatic(
              Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
              Utils.kFunctionToASMMethod(Runtime::onNanoTime))
        } else {
          super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
      }
    }
  }
}
