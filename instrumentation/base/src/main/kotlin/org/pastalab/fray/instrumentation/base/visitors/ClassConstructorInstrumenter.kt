package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9

class ClassConstructorInstrumenter(cv: ClassVisitor, val isJDK: Boolean) : ClassVisitor(ASM9, cv) {
  var className = ""

  override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String?>?,
  ) {
    className = name
    super.visit(version, access, name, signature, superName, interfaces)
  }

  override fun visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?,
  ): MethodVisitor {
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    if (name == "<clinit>") {
      if (isJDK && ALLOWED_JDK_CLASSES.none { className.contains(it) }) {
        return mv
      }
      val methodSignature = "#$name$descriptor"
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onSkipScheduling,
              access,
              name,
              descriptor,
              loadThis = false,
              loadArgs = false,
              preCustomizer = { it.push(methodSignature) },
          )
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSkipSchedulingDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className,
          customizer = { mv, isFinalBlock -> mv.push(methodSignature) },
      )
    }
    return mv
  }

  companion object {
    val ALLOWED_JDK_CLASSES =
        arrayOf(
            "sun/security/ssl/SSLExtension\$ClientExtensions",
            "sun/security/ssl/SSLContextImpl",
            "sun/security/validator/CADistrustPolicy",
            "sun/security/util/UntrustedCertificates",
            "sun/nio/cs/ThreadLocalCoders",
            "java/net/IDN",
            "sun/security/ssl/ClientHandshakeContext",
            "java/security/cert/X509CertSelector",
            "sun/net/httpserver/",
        )
  }
}
