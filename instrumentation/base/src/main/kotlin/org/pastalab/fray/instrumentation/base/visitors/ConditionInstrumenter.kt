package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class ConditionInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(
        cv,
        java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject::class.java.name) {

  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if ((name == "await" || name == "awaitUninterruptibly") && descriptor == "()V") {
      val method =
          if (name == "await") {
            Pair(
                org.pastalab.fray.runtime.Runtime::onConditionAwait,
                org.pastalab.fray.runtime.Runtime::onConditionAwaitDone)
          } else {
            Pair(
                org.pastalab.fray.runtime.Runtime::onConditionAwaitUninterruptibly,
                org.pastalab.fray.runtime.Runtime::onConditionAwaitUninterruptiblyDone)
          }
      val eMv = MethodEnterVisitor(mv, method.first, access, name, descriptor, true, false)
      return MethodExitVisitor(
          eMv, method.second, access, name, descriptor, true, false, true, className)
    }
    if (name == "signal") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onConditionSignal,
              access,
              name,
              descriptor,
              true,
              false)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onConditionSignalDone,
          access,
          name,
          descriptor,
          true,
          false,
          true,
          className)
    }
    if (name == "signalAll") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onConditionSignalAll,
              access,
              name,
              descriptor,
              true,
              false)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onConditionSignalDone,
          access,
          name,
          descriptor,
          true,
          false,
          true,
          className)
    }
    return mv
  }
}
