package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
import java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class ConditionInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, ConditionObject::class.java.name) {

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
            Pair(Runtime::onConditionAwait, Runtime::onConditionAwaitDone)
          } else {
            Pair(
                Runtime::onConditionAwaitUninterruptibly,
                Runtime::onConditionAwaitUninterruptiblyDone)
          }
      val eMv = MethodEnterVisitor(mv, method.first, access, name, descriptor, true, false)
      return MethodExitVisitor(eMv, method.second, access, name, descriptor, true, false, true)
    }
    if (name == "signal") {
      val eMv =
          MethodEnterVisitor(mv, Runtime::onConditionSignal, access, name, descriptor, true, false)
      return MethodExitVisitor(
          eMv, Runtime::onConditionSignalDone, access, name, descriptor, true, false, true)
    }
    if (name == "signalAll") {
      val eMv =
          MethodEnterVisitor(
              mv, Runtime::onConditionSignalAll, access, name, descriptor, true, false)
      return MethodExitVisitor(
          eMv, Runtime::onConditionSignalDone, access, name, descriptor, true, false, true)
    }
    return mv
  }
}
