package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
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
          MethodEnterVisitor(mv, Runtime::onSkipMethod, access, name, descriptor, false, false, preCustomizer = {
            it.push(methodSignature)
          })
      return MethodExitVisitor(
          eMv,
          Runtime::onSkipMethodDone,
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
