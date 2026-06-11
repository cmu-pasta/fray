package org.pastalab.fray.instrumentation.base.visitors

import java.util.concurrent.locks.LockSupport
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class LockSupportInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, LockSupport::class.java.name) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?,
  ): MethodVisitor {
    if (name == "park") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onThreadPark,
              access,
              name,
              descriptor,
              loadThis = false,
              loadArgs = false,
          )
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onThreadParkDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className,
      )
    }
    if (name.startsWith("unpark")) {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onThreadUnpark,
              access,
              name,
              descriptor,
              loadThis = false,
              loadArgs = true,
          )
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onThreadUnparkDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = true,
          addFinalBlock = true,
          thisType = className,
      )
    }
    return mv
  }
}
