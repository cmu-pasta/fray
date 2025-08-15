package org.pastalab.fray.instrumentation.base.visitors

import java.util.concurrent.Semaphore
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class SemaphoreInstrumenter(cv: ClassVisitor) : ClassVisitorBase(cv, Semaphore::class.java.name) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "<init>") {
      return MethodExitVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onSemaphoreInit,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className)
    }
    if ((name == "acquire" || name == "acquireUninterruptibly" || name == "tryAcquire")) {
      val method =
          if (name == "acquire") {
            if (descriptor.startsWith("()")) {
              org.pastalab.fray.runtime.Runtime::onSemaphoreAcquire
            } else {
              org.pastalab.fray.runtime.Runtime::onSemaphoreAcquirePermits
            }
          } else if (name == "acquireUninterruptibly") {
            if (descriptor.startsWith("()")) {
              org.pastalab.fray.runtime.Runtime::onSemaphoreAcquireUninterruptibly
            } else {
              org.pastalab.fray.runtime.Runtime::onSemaphoreAcquirePermitsUninterruptibly
            }
          } else {
            if (descriptor.startsWith("()")) {
              org.pastalab.fray.runtime.Runtime::onSemaphoreTryAcquire
            } else if (descriptor.startsWith("(I)")) {
              org.pastalab.fray.runtime.Runtime::onSemaphoreTryAcquirePermits
            } else if (descriptor.startsWith("(J")) {
              org.pastalab.fray.runtime.Runtime::onSemaphoreTryAcquireTimeout
            } else {
              org.pastalab.fray.runtime.Runtime::onSemaphoreTryAcquirePermitsTimeout
            }
          }
      val eMv =
          MethodEnterVisitor(
              mv, method, access, name, descriptor, loadThis = true, loadArgs = true) {
                if (name == "tryAcquire") {
                  // We need to override the timeout value for controlled concurrency.
                  if (descriptor.startsWith("(IJ")) {
                    storeArg(1)
                  } else if (descriptor.startsWith("(J")) {
                    storeArg(0)
                  }
                }
              }
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSemaphoreAcquireDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className)
    }
    if (name == "release" && descriptor == "()V") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onSemaphoreRelease,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = true)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSemaphoreReleaseDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className)
    }
    if (name == "release" && descriptor == "(I)V") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onSemaphoreReleasePermits,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = true)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSemaphoreReleaseDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className)
    }
    if (name == "drainPermits") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onSemaphoreDrainPermits,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSemaphoreDrainPermitsDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className)
    }
    if (name == "reducePermits") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onSemaphoreReducePermits,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = true)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSemaphoreReducePermitsDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className)
    }
    return mv
  }
}
