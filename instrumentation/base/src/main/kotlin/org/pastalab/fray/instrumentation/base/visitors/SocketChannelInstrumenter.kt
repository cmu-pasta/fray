package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.pastalab.fray.runtime.Runtime

class SocketChannelInstrumenter(cv: ClassVisitor) :
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
              mv,
              Runtime::onSocketChannelConnect,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = true)
      return MethodExitVisitor(
          eMv,
          Runtime::onSocketChannelConnectDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className) { mv, isFinalBlock ->
            if (isFinalBlock) {
              push(false)
            } else {
              dup2()
              pop()
            }
          }
    }
    if (name == "finishConnect") {
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onSocketChannelFinishConnect,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false)
      return MethodExitVisitor(
          eMv,
          Runtime::onSocketChannelFinishConnectDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className) { mv, isFinalBlock ->
            if (isFinalBlock) {
              push(false)
            } else {
              dup2()
              pop()
            }
          }
    }
    if (name == "write" && descriptor.startsWith("(Ljava/nio/ByteBuffer;)")) {
      return MethodExitVisitor(
          mv,
          Runtime::onSocketChannelWriteDoneInt,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className) { mv, isFinalBlock ->
            if (isFinalBlock) {
              push(0)
              loadThis()
            } else {
              dup()
              loadThis()
            }
          }
    }
    if (name == "write" && descriptor.startsWith("([Ljava/nio/ByteBuffer;II)")) {
      return MethodExitVisitor(
          mv,
          Runtime::onSocketChannelWriteDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className) { mv, isFinalBlock ->
            if (isFinalBlock) {
              push(0L)
              loadThis()
            } else {
              dup2()
              loadThis()
            }
          }
    }
    if (name == "read" && descriptor.startsWith("(Ljava/nio/ByteBuffer;)")) {
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onSocketChannelRead,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false)
      return MethodExitVisitor(
          eMv,
          Runtime::onSocketChannelReadDoneInt,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className) { mv, isFinalBlock ->
            if (isFinalBlock) {
              push(0)
              loadThis()
            } else {
              dup()
              loadThis()
            }
          }
    }
    if (name == "read" && descriptor.startsWith("([Ljava/nio/ByteBuffer;II)")) {
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onSocketChannelRead,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false)
      return MethodExitVisitor(
          eMv,
          Runtime::onSocketChannelReadDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className) { mv, isFinalBlock ->
            if (isFinalBlock) {
              push(0L)
              loadThis()
            } else {
              dup2()
              loadThis()
            }
          }
    }
    return mv
  }
}
