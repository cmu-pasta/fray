package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
import java.util.concurrent.CountDownLatch
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class CountDownLatchInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, CountDownLatch::class.java.name) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "await") {
      val eMv = MethodEnterVisitor(mv, Runtime::onLatchAwait, access, name, descriptor, true, false)
      return MethodExitVisitor(
          eMv, Runtime::onLatchAwaitDone, access, name, descriptor, true, false, true)
    }
    if (name == "countDown") {
      val eMv =
          MethodEnterVisitor(mv, Runtime::onLatchCountDown, access, name, descriptor, true, false)
      return MethodExitVisitor(
          eMv, Runtime::onLatchCountDownDone, access, name, descriptor, true, false, true)
    }
    return super.instrumentMethod(mv, access, name, descriptor, signature, exceptions)
  }
}
