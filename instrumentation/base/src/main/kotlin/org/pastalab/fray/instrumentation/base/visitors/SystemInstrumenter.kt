package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor

class SystemInstrumenter(cv: ClassVisitor) : ClassVisitorBase(cv, System::class.java.name) {
  override fun instrumentMethod(
      mv: org.objectweb.asm.MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): org.objectweb.asm.MethodVisitor {
    if (name == "getProperty" || name == "setProperty") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onSkipMethod,
              access,
              name,
              descriptor,
              false,
              false,
              preCustomizer = { push(name) })
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSkipMethodDone,
          access,
          name,
          descriptor,
          false,
          false,
          true,
          customizer = { mv, isFinalBlock -> push(name) })
    } else {
      return mv
    }
  }
}
