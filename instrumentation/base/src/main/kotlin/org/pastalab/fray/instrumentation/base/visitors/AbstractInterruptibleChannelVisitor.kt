package org.pastalab.fray.instrumentation.base.visitors

import java.nio.channels.spi.AbstractInterruptibleChannel
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.pastalab.fray.runtime.Runtime

class AbstractInterruptibleChannelVisitor(cv: ClassVisitor) :
    ClassVisitorBase(cv, AbstractInterruptibleChannel::class.java.name) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?,
  ): MethodVisitor {
    if (name == "close") {
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onSocketChannelClose,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false,
          )
      return MethodExitVisitor(
          eMv,
          Runtime::onSocketChannelCloseDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className,
      )
    }
    return mv
  }
}
