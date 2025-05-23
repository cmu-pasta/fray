package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.commons.AdviceAdapter

class SynchronizedMethodInstrumenter(cv: ClassVisitor, private val instrumentingJdk: Boolean) :
    ClassVisitor(ASM9, cv) {
  var className = ""
  var shouldInstrument = !instrumentingJdk

  override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?
  ) {
    super.visit(version, access, name, signature, superName, interfaces)
    className = name
    if (instrumentingJdk) {
      shouldInstrument =
          name.startsWith("java/util/Observable") ||
              name.startsWith("java/util/Vector") ||
              name.startsWith("java/util/logging/") ||
              name.startsWith("sun/nio/ch/") ||
              name.startsWith("sun/security/")
    }
  }

  override fun visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {

    if (access and Opcodes.ACC_NATIVE != 0 ||
        access and Opcodes.ACC_SYNCHRONIZED == 0 ||
        !shouldInstrument) {
      return super.visitMethod(access, name, descriptor, signature, exceptions)
    }
    val newAccess = access and Opcodes.ACC_SYNCHRONIZED.inv()
    val mv = super.visitMethod(newAccess, name, descriptor, signature, exceptions)
    return object : AdviceAdapter(ASM9, mv, newAccess, name, descriptor) {
      val enterLabel = Label()

      override fun onMethodEnter() {
        super.onMethodEnter()
        if (access and Opcodes.ACC_STATIC != 0) {
          push(Type.getObjectType(className))
        } else {
          loadThis()
        }
        dup()
        super.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
            org.pastalab.fray.runtime.Runtime::onMonitorEnter.name,
            Utils.kFunctionToJvmMethodDescriptor(org.pastalab.fray.runtime.Runtime::onMonitorEnter),
            false)
        visitLabel(enterLabel)
        visitInsn(MONITORENTER)
      }

      fun insertMonitorExit() {
        if (access and Opcodes.ACC_STATIC != 0) {
          push(Type.getObjectType(className))
        } else {
          loadThis()
        }
        dup()
        dup()
        super.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
            org.pastalab.fray.runtime.Runtime::onMonitorExit.name,
            Utils.kFunctionToJvmMethodDescriptor(org.pastalab.fray.runtime.Runtime::onMonitorExit),
            false)
        visitInsn(MONITOREXIT)
        super.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
            org.pastalab.fray.runtime.Runtime::onMonitorExitDone.name,
            Utils.kFunctionToJvmMethodDescriptor(
                org.pastalab.fray.runtime.Runtime::onMonitorExitDone),
            false)
      }

      override fun onMethodExit(opcode: Int) {
        if (opcode != ATHROW) {
          insertMonitorExit()
        }
        super.onMethodExit(opcode)
      }

      override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        val label = mark()
        catchException(enterLabel, label, Type.getObjectType("java/lang/Throwable"))
        val locals = getLocals()
        visitFrame(Opcodes.F_NEW, locals.size, getLocals(), 1, arrayOf("java/lang/Throwable"))
        insertMonitorExit()
        visitInsn(ATHROW)
        super.visitMaxs(maxStack, maxLocals)
      }
    }
  }
}
