package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class MethodHandleNativesInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, "java.lang.invoke.MethodHandleNatives") {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "linkMethodHandleConstant") {
      val methodSignature = "$className#$name$descriptor"
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onSkipPrimitive,
              access,
              name,
              descriptor,
              false,
              false,
              preCustomizer = { it.push(methodSignature) })
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSkipPrimitiveDone,
          access,
          name,
          descriptor,
          false,
          false,
          true,
          className // pass thisType
          ) { mv, isFinalBlock ->
            mv.push(methodSignature)
          }
    }
    return super.instrumentMethod(mv, access, name, descriptor, signature, exceptions)
  }
}
