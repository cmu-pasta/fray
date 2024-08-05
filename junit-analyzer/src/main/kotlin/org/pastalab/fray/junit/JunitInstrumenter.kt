package org.pastalab.fray.junit

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter
import org.pastalab.fray.instrumentation.visitors.ClassVisitorBase

class JunitInstrumenter(cv: ClassVisitor) : ClassVisitorBase(cv, "junit/framework/TestCase") {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "run" && descriptor.startsWith("(Ljunit/framework/TestResult;)V")) {
      return object : AdviceAdapter(ASM9, mv, access, name, descriptor) {
        override fun onMethodEnter() {
          mv.visitVarInsn(ALOAD, 0)
          mv.visitMethodInsn(
              INVOKESTATIC,
              "org.pastalab/fray/junit/Recorder",
              "testStart",
              "(Ljunit/framework/TestCase;)V",
              false)
        }

        override fun onMethodExit(opcode: Int) {
          mv.visitVarInsn(ALOAD, 0)
          mv.visitMethodInsn(
              INVOKESTATIC,
              "org.pastalab/fray/junit/Recorder",
              "testEnd",
              "(Ljunit/framework/TestCase;)V",
              false)
        }
      }
    }
    return mv
  }
}
