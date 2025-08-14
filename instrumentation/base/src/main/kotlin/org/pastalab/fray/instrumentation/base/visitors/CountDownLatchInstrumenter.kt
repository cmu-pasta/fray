package org.pastalab.fray.instrumentation.base.visitors

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
    if (name == "await" && descriptor == "()V") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onLatchAwait,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onLatchAwaitDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className)
    }
    if (name == "countDown") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onLatchCountDown,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onLatchCountDownDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className)
    }
    return super.instrumentMethod(mv, access, name, descriptor, signature, exceptions)
  }
}
