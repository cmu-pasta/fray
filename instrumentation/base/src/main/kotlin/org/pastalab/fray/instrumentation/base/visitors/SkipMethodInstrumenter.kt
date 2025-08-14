package org.pastalab.fray.instrumentation.base.visitors

import kotlin.reflect.KFunction
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

open class SkipMethodInstrumenter(
    cv: ClassVisitor,
    val skipMethod: KFunction<*>,
    val skipDoneMethod: KFunction<*>,
    vararg classNames: String
) : ClassVisitorBase(cv, *classNames) {
  override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?
  ) {
    super.visit(version, access, name, signature, superName, interfaces)
    shouldInstrument = shouldInstrument or classNames.any { name.startsWith(it) }
  }

  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "<init>" || name == "<clinit>") {
      return mv
    }
    val methodSignature = "$className#$name$descriptor"
    val eMv =
        MethodEnterVisitor(
            mv,
            skipMethod,
            access,
            name,
            descriptor,
            loadThis = false,
            loadArgs = false,
            preCustomizer = { push(methodSignature) })
    return MethodExitVisitor(
        eMv,
        skipDoneMethod,
        access,
        name,
        descriptor,
        loadThis = false,
        loadArgs = false,
        addFinalBlock = true,
        thisType = className,
        customizer = { mv, isFinalBlock -> push(methodSignature) })
  }
}
