package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter

class ThreadParkInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
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
        if (owner == "java/lang/Thread" && (name == "parkNanos" || name == "parkUntil")) {
          val method =
              if (name == "parkNanos") {
                if (descriptor == "(J)V") {
                  Runtime::onThreadParkNanos
                } else {
                  Runtime::onThreadParkNanosWithBlocker
                }
              } else {
                if (descriptor == "(J)V") {
                  Runtime::onThreadParkUntil
                } else {
                  Runtime::onThreadParkUntilWithBlocker
                }
              }
          invokeStatic(
              Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
              Utils.kFunctionToASMMethod(method))
          return
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      }
    }
  }
}
