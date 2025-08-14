package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.pastalab.fray.runtime.Runtime

class LoadClassInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  var className = ""

  override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String?>?
  ) {
    className = name
    super.visit(version, access, name, signature, superName, interfaces)
  }

  override fun visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String?>?
  ): MethodVisitor? {
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    if (name == "loadClass" &&
        (descriptor == "(Ljava/lang/String;)Ljava/lang/Class;" ||
            descriptor == "(Ljava/lang/String;Z)Ljava/lang/Class;")) {
      val methodSignature = "$className#$name$descriptor"
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onSkipScheduling,
              access,
              name,
              descriptor,
              loadThis = false,
              loadArgs = false,
              preCustomizer = { push(methodSignature) })
      return MethodExitVisitor(
          eMv,
          Runtime::onSkipSchedulingDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className,
          customizer = { mv, isFinalBlock -> push(methodSignature) })
    }
    return mv
  }
}
