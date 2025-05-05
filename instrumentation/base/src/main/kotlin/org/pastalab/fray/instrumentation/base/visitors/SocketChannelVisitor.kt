package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.pastalab.fray.runtime.Runtime

class SocketChannelVisitor(cv: ClassVisitor) :
    ClassVisitorBase(
        cv,
        "sun/nio/ch/SocketChannelImpl",
    ) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "connect" && descriptor.startsWith("(Ljava/net/SocketAddress;)")) {

      val eMv =
          MethodEnterVisitor(
              mv, Runtime::onSocketChannelConnect, access, name, descriptor, true, true)
      return MethodExitVisitor(
          eMv, Runtime::onSocketChannelConnectDone, access, name, descriptor, true, false, true) {
              mv,
              isFinalBlock ->
            if (isFinalBlock) {
              push(false)
            } else {
              dup2()
              pop()
            }
          }
    }
    return mv
  }
}
