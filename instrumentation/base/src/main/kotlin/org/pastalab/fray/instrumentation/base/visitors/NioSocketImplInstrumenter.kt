package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.pastalab.fray.runtime.Runtime

class NioSocketImplInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, "sun.nio.ch.NioSocketImpl") {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "connect" && descriptor.startsWith("(Ljava/net/SocketAddress;I")) {
      val eMv =
          MethodEnterVisitor(mv, Runtime::onNioSocketConnect, access, name, descriptor, true, false)
      return MethodExitVisitor(
          eMv,
          Runtime::onNioSocketConnectDone,
          access,
          name,
          descriptor,
          true,
          false,
          true,
          className)
    } else if (name == "read") {
      val eMv =
          MethodEnterVisitor(mv, Runtime::onNioSocketRead, access, name, descriptor, true, false)
      return MethodExitVisitor(
          eMv,
          Runtime::onNioSocketReadDone,
          access,
          name,
          descriptor,
          true,
          false,
          true,
          className) { mv, isFinalBlock ->
            if (isFinalBlock) {
              push(0)
            } else {
              dup2()
              pop()
            }
          }
    }
    return mv
  }
}
