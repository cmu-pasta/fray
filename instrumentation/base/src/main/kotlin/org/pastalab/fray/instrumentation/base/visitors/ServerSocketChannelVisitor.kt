package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACONST_NULL
import org.pastalab.fray.runtime.Runtime

class ServerSocketChannelVisitor(cv: ClassVisitor) :
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
          mv, Runtime::onServerSocketChannelBindDone, access, name, descriptor, true, false, false)
    }
    if (name == "accept") {
      val eMv =
          MethodEnterVisitor(
              mv, Runtime::onServerSocketChannelAccept, access, name, descriptor, true, false)
      return MethodExitVisitor(
          eMv,
          Runtime::onServerSocketChannelAcceptDone,
          access,
          name,
          descriptor,
          true,
          false,
          true) { mv, isFinalBlock ->
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
