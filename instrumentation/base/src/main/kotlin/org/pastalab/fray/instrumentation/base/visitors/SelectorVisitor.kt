package org.pastalab.fray.instrumentation.base.visitors

import java.nio.channels.Selector
import java.nio.channels.spi.AbstractSelector
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.pastalab.fray.runtime.Runtime

class SelectorVisitor(cv: ClassVisitor) :
    ClassVisitorBase(
        cv,
        "sun/nio/ch/SelectorImpl",
        "sun/nio/ch/KQueueSelectorImpl",
        "sun/nio/ch/EPollSelectorImpl",
        AbstractSelector::class.java.name,
        Selector::class.java.name) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "lockAndDoSelect" && access and Opcodes.ACC_ABSTRACT == 0) {
      val eMv =
          MethodEnterVisitor(mv, Runtime::onSelectorSelect, access, name, descriptor, true, false)
      return MethodExitVisitor(
          eMv,
          Runtime::onSelectorSelectDone,
          access,
          name,
          descriptor,
          true,
          false,
          true,
          className)
    }
    if (name == "close" && access and Opcodes.ACC_ABSTRACT == 0) {
      val eMv =
          MethodEnterVisitor(mv, Runtime::onSelectorClose, access, name, descriptor, true, false)
      return MethodExitVisitor(
          eMv, Runtime::onSelectorCloseDone, access, name, descriptor, true, false, true, className)
    }
    if (name == "setEventOps" && access and Opcodes.ACC_ABSTRACT == 0) {
      return MethodExitVisitor(
          mv,
          Runtime::onSelectorSetEventOpsDone,
          access,
          name,
          descriptor,
          true,
          true,
          false,
          className)
    }
    if (name == "cancel" && access and Opcodes.ACC_ABSTRACT == 0) {
      return MethodExitVisitor(
          mv,
          Runtime::onSelectorCancelKeyDone,
          access,
          name,
          descriptor,
          true,
          true,
          false,
          className)
    }
    if (name == "open") {
      val eMv =
          MethodEnterVisitor(mv, Runtime::onSelectorOpen, access, name, descriptor, false, false)
      return MethodExitVisitor(
          eMv, Runtime::onSelectorOpenDone, access, name, descriptor, false, false, true, className)
    }
    return mv
  }
}
