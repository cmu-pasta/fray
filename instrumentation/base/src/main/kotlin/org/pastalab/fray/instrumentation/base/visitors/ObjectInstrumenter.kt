package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.pastalab.fray.runtime.Runtime

class ObjectInstrumenter(cv: ClassVisitor) : ClassVisitorBase(cv, Object::class.java.name) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "wait" && descriptor == "(J)V") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onObjectWait,
              access,
              name,
              descriptor,
              true,
              true,
          )
      return MethodExitVisitor(
          eMv, Runtime::onObjectWaitDone, access, name, descriptor, true, false, true)
    }
    return mv
  }
}
