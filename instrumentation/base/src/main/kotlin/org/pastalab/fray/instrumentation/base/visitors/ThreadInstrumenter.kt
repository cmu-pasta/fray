package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter

class ThreadInstrumenter(cv: ClassVisitor) : ClassVisitorBase(cv, Thread::class.java.name) {

  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "<init>" &&
        descriptor == "(Ljava/lang/ThreadGroup;Ljava/lang/String;ILjava/lang/Runnable;J)V") {
      return MethodExitVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onThreadCreateDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className)
    }
    if (name == "start") {
      val eMv =
          MethodExitVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onThreadStartDone,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false,
              addFinalBlock = true,
              thisType = className)
      return MethodEnterVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onThreadStart,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false)
    }
    if (name == "yield") {
      return MethodEnterVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onYield,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false)
    }
    if (name == "getAndClearInterrupt") {
      return MethodExitVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onThreadGetAndClearInterrupt,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className)
    }
    if (name == "isInterrupted") {
      return MethodExitVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onThreadIsInterrupted,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className)
    }
    if (name == "clearInterrupt") {
      return MethodExitVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onThreadClearInterrupt,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className)
    }
    if (name == "setInterrupt") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onThreadInterrupt,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onThreadInterruptDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className)
    }
    if (name == "getState") {
      return MethodExitVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onThreadGetState,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className)
    }

    if (name == "interrupt") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onThreadInterrupt,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false)

      return object : GeneratorAdapter(ASM9, eMv, access, name, descriptor) {

        override fun visitMethodInsn(
            opcode: Int,
            owner: String,
            name: String,
            descriptor: String,
            isInterface: Boolean
        ) {
          super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
          if (name == "interrupt0") {
            loadThis()
            invokeStatic(
                Type.getObjectType(
                    org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/")),
                Utils.kFunctionToASMMethod(
                    org.pastalab.fray.runtime.Runtime::onThreadInterruptDone),
            )
          }
        }
      }
    }
    return mv
  }
}
