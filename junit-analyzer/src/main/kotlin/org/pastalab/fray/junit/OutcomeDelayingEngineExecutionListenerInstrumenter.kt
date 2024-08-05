package org.pastalab.fray.junit

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.pastalab.fray.instrumentation.visitors.ClassVisitorBase
import org.pastalab.fray.instrumentation.visitors.Utils

class OutcomeDelayingEngineExecutionListenerInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(
        cv, "org.junit.platform.launcher.core.OutcomeDelayingEngineExecutionListener") {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "executionStarted") {
      return object : AdviceAdapter(ASM9, mv, access, name, descriptor) {
        override fun onMethodEnter() {
          loadArgs()
          invokeStatic(
              Type.getObjectType(Recorder::class.java.name.replace(".", "/")),
              Utils.kFunctionToASMMethod(Recorder::executionStarted),
          )
        }
      }
    }
    if (name == "executionFinished") {
      return object : AdviceAdapter(ASM9, mv, access, name, descriptor) {
        override fun onMethodEnter() {
          loadArgs()
          invokeStatic(
              Type.getObjectType(Recorder::class.java.name.replace(".", "/")),
              Utils.kFunctionToASMMethod(Recorder::executionFinished),
          )
        }
      }
    }
    return mv
  }
}
