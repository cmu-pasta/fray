package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACONST_NULL
import org.pastalab.fray.runtime.Runtime

class ServerSocketChannelInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(
        cv,
        "sun/nio/ch/ServerSocketChannelImpl",
    ) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "bind" && descriptor.startsWith("(Ljava/net/SocketAddress;I)")) {
      return MethodExitVisitor(
          mv,
          Runtime::onServerSocketChannelBindDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className)
    }
    if (name == "accept") {
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onServerSocketChannelAccept,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false)
      return MethodExitVisitor(
          eMv,
          Runtime::onServerSocketChannelAcceptDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className) { mv, isFinalBlock ->
            if (isFinalBlock) {
              visitInsn(ACONST_NULL)
            } else {
              dup2()
              pop()
            }
          }
    }
    return mv
  }
}
