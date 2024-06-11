package cmu.pasta.fray.junit

import cmu.pasta.fray.instrumentation.visitors.ClassVisitorBase
import cmu.pasta.fray.instrumentation.visitors.MethodEnterVisitor
import cmu.pasta.fray.instrumentation.visitors.MethodExitVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter

class JunitInstrumenter(cv: ClassVisitor): ClassVisitorBase(cv, "junit/framework/TestCase") {
    override fun instrumentMethod(
        mv: MethodVisitor,
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
      if (name == "run" && descriptor.startsWith("(Ljunit/framework/TestResult;)V")) {
        return object: AdviceAdapter(ASM9, mv, access, name, descriptor) {
          override fun onMethodEnter() {
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(INVOKESTATIC, "cmu/pasta/fray/junit/Recorder", "testStart", "(Ljunit/framework/TestCase;)V", false)
          }
          override fun onMethodExit(opcode: Int) {
            mv.visitVarInsn(ALOAD, 0)
            mv.visitMethodInsn(INVOKESTATIC, "cmu/pasta/fray/junit/Recorder", "testEnd", "(Ljunit/framework/TestCase;)V", false)
          }
        }
      }
      return mv
    }
}
