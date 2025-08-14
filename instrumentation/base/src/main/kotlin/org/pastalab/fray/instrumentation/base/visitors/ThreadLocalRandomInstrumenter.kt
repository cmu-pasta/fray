package org.pastalab.fray.instrumentation.base.visitors

import java.util.concurrent.ThreadLocalRandom
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.pastalab.fray.runtime.Runtime

class ThreadLocalRandomInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, ThreadLocalRandom::class.java.name) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "getProbe") {
      return MethodExitVisitor(
          mv,
          Runtime::onThreadLocalRandomGetProbe,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className)
    }
    return mv
  }
}
