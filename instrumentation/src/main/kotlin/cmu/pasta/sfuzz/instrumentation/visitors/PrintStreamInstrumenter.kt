package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
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
      val eMv =
          MethodEnterVisitor(mv, Runtime::onSkipMethod, access, name, descriptor, false, false)
      return MethodExitVisitor(
          eMv, Runtime::onSkipMethodDone, access, name, descriptor, false, false, false)
    }
    return super.instrumentMethod(mv, access, name, descriptor, signature, exceptions)
  }
}
