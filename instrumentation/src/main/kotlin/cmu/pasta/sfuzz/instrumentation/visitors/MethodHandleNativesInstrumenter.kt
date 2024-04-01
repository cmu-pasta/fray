package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class MethodHandleNativesInstrumenter(cv: ClassVisitor): ClassVisitorBase(cv, "java.lang.invoke.MethodHandleNatives") {
  override fun instrumentMethod(
    mv: MethodVisitor,
    access: Int,
    name: String,
    descriptor: String,
    signature: String?,
    exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "linkMethodHandleConstant") {
      val eMv = MethodEnterVisitor(mv, Runtime::onSkipMethod, access, name, descriptor, false, false)
      return MethodExitVisitor(eMv, Runtime::onSkipMethodDone, access, name, descriptor, false, false, true)
    }
    return super.instrumentMethod(mv, access, name, descriptor, signature, exceptions)
  }
}
