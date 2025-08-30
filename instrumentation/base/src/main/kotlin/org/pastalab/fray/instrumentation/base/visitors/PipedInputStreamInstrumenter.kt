package org.pastalab.fray.instrumentation.base.visitors

import java.io.PipedInputStream
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.pastalab.fray.runtime.Runtime

class PipedInputStreamInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, PipedInputStream::class.java.name) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "read" || name == "receive") {
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onPipedInputStreamRead,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false)
      return MethodExitVisitor(
          eMv,
          Runtime::onPipedInputStreamReadDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className)
    } else {
      return mv
    }
  }
}
