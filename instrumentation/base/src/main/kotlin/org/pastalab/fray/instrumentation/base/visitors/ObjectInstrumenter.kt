package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

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
              false)
      // We cannot use MethodExitVisitor here because `onObjectWaitDone` may throw an
      // `InterruptedException`
      // So we cannot catch that exception twice.
      return object : AdviceAdapter(ASM9, eMv, access, name, descriptor) {
        val methodEnterLabel = Label()
        val methodExitLabel = Label()

        override fun onMethodEnter() {
          super.onMethodEnter()
          visitLabel(methodEnterLabel)
        }

        override fun onMethodExit(opcode: Int) {
          if (opcode != ATHROW) {
            visitLabel(methodExitLabel)
            loadThis()
            invokeStatic(
                Type.getObjectType(
                    org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/")),
                Utils.kFunctionToASMMethod(org.pastalab.fray.runtime.Runtime::onObjectWaitDone))
          }
          super.onMethodExit(opcode)
        }

        override fun visitMaxs(maxStack: Int, maxLocals: Int) {
          catchException(
              methodEnterLabel, methodExitLabel, Type.getObjectType("java/lang/Throwable"))
          val locals = getLocals()
          visitFrame(Opcodes.F_NEW, locals.size, getLocals(), 1, arrayOf("java/lang/Throwable"))
          loadThis()
          invokeStatic(
              Type.getObjectType(
                  org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/")),
              Utils.kFunctionToASMMethod(org.pastalab.fray.runtime.Runtime::onObjectWaitDone))
          visitInsn(ATHROW)
          super.visitMaxs(maxStack, maxLocals)
        }
      }
    }
    return mv
  }
}
