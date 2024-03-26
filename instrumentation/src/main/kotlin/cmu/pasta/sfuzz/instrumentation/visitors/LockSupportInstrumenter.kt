package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
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
      exceptions: Array<out String>?
  ): MethodVisitor {
    var mv = super.instrumentMethod(mv, access, name, descriptor, signature, exceptions)
    if (name.startsWith("park")) {
      val eMv =
          MethodEnterVisitor(mv, Runtime::onThreadPark, access, name, descriptor, false, false)
      return MethodExitVisitor(
          eMv, Runtime::onThreadParkDone, access, name, descriptor, false, false, true)
    }
    if (name.startsWith("unpark")) {
      val eMv =
          MethodEnterVisitor(mv, Runtime::onThreadUnpark, access, name, descriptor, false, true)
      return MethodExitVisitor(
          eMv, Runtime::onThreadUnparkDone, access, name, descriptor, false, true, true)
    }
    return mv
  }
}
