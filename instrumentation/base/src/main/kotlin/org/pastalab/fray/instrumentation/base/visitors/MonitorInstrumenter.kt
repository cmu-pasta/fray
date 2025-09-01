package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.ASM9

class MonitorInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  var className = ""

  override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?
  ) {
    className = name
    super.visit(version, access, name, signature, superName, interfaces)
  }

  override fun visitMethod(
      access: Int,
      name: String?,
      descriptor: String?,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (((className.startsWith("jdk/internal/") ||
        // PipedInputStream is handled separately through [PipedInputStreamInstrumenter].
        className == "java/io/PipedInputStream") && !className.startsWith("jdk/internal/net")) ||
        access and Opcodes.ACC_NATIVE != 0) {
      return super.visitMethod(access, name, descriptor, signature, exceptions)
    }
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    return object : MethodVisitor(ASM9, mv) {
      override fun visitInsn(opcode: Int) {
        if (opcode == Opcodes.MONITORENTER || opcode == Opcodes.MONITOREXIT) {
          if (opcode == Opcodes.MONITORENTER) {
            super.visitInsn(Opcodes.DUP)
            super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
                org.pastalab.fray.runtime.Runtime::onMonitorEnter.name,
                Utils.kFunctionToJvmMethodDescriptor(
                    org.pastalab.fray.runtime.Runtime::onMonitorEnter),
                false)
            super.visitInsn(opcode)
          } else {
            super.visitInsn(Opcodes.DUP)
            super.visitInsn(Opcodes.DUP)
            super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
                org.pastalab.fray.runtime.Runtime::onMonitorExit.name,
                Utils.kFunctionToJvmMethodDescriptor(
                    org.pastalab.fray.runtime.Runtime::onMonitorExit),
                false)
            super.visitInsn(opcode)
            super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
                org.pastalab.fray.runtime.Runtime::onMonitorExitDone.name,
                Utils.kFunctionToJvmMethodDescriptor(
                    org.pastalab.fray.runtime.Runtime::onMonitorExitDone),
                false)
          }
        } else {
          super.visitInsn(opcode)
        }
      }
    }
  }
}
