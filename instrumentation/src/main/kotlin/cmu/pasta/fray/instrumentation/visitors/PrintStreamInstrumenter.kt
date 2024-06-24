package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
import java.io.PrintStream
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class PrintStreamInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, PrintStream::class.java.name) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "println" || name == "writeln" || name == "write") {
      val methodSignature = "$className#$name$descriptor"
      val eMv =
          MethodEnterVisitor(mv, Runtime::onSkipMethod, access, name, descriptor, false, false, preCustomizer = {
            it.push(methodSignature)
          })
      return MethodExitVisitor(
          eMv, Runtime::onSkipMethodDone, access, name, descriptor, false, false, true) {
            it.push(methodSignature)
          }
    }
    return super.instrumentMethod(mv, access, name, descriptor, signature, exceptions)
  }
}
