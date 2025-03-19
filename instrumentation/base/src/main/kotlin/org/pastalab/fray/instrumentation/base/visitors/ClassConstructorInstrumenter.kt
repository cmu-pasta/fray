package org.anonlab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9

class ClassConstructorInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  override fun visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    if (name == "<clinit>") {
      val methodSignature = "#$name$descriptor"
      val eMv =
          MethodEnterVisitor(
              mv,
              org.anonlab.fray.runtime.Runtime::onSkipMethod,
              access,
              name,
              descriptor,
              false,
              false,
              preCustomizer = { it.push(methodSignature) })
      return MethodExitVisitor(
          eMv,
          org.anonlab.fray.runtime.Runtime::onSkipMethodDone,
          access,
          name,
          descriptor,
          false,
          false,
          true,
          { it.push(methodSignature) })
    }
    return mv
  }
}
