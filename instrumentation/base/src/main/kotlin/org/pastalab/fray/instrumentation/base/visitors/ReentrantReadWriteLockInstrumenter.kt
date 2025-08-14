package org.pastalab.fray.instrumentation.base.visitors

import java.util.concurrent.locks.ReentrantReadWriteLock
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class ReentrantReadWriteLockInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, ReentrantReadWriteLock::class.java.name) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "<init>" && descriptor == "(Z)V") {
      return MethodExitVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onReentrantReadWriteLockInit,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className)
    }
    return mv
  }
}
