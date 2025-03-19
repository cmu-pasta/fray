package org.anonlab.fray.instrumentation.base.visitors

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
          org.anonlab.fray.runtime.Runtime::onSemaphoreInit,
          access,
          name,
          descriptor,
          true,
          false,
          false)
    }
    if ((name == "acquire" || name == "acquireUninterruptibly" || name == "tryAcquire")) {
      val method =
          if (name == "acquire") {
            if (descriptor.startsWith("()")) {
              org.anonlab.fray.runtime.Runtime::onSemaphoreAcquire
            } else {
              org.anonlab.fray.runtime.Runtime::onSemaphoreAcquirePermits
            }
          } else if (name == "acquireUninterruptibly") {
            if (descriptor.startsWith("()")) {
              org.anonlab.fray.runtime.Runtime::onSemaphoreAcquireUninterruptibly
            } else {
              org.anonlab.fray.runtime.Runtime::onSemaphoreAcquirePermitsUninterruptibly
            }
          } else {
            if (descriptor.startsWith("()")) {
              org.anonlab.fray.runtime.Runtime::onSemaphoreTryAcquire
            } else if (descriptor.startsWith("(I)")) {
              org.anonlab.fray.runtime.Runtime::onSemaphoreTryAcquirePermits
            } else if (descriptor.startsWith("(J")) {
              org.anonlab.fray.runtime.Runtime::onSemaphoreTryAcquireTimeout
            } else {
              org.anonlab.fray.runtime.Runtime::onSemaphoreTryAcquirePermitsTimeout
            }
          }
      val eMv =
          MethodEnterVisitor(mv, method, access, name, descriptor, true, true) {
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
          org.anonlab.fray.runtime.Runtime::onSemaphoreAcquireDone,
          access,
          name,
          descriptor,
          false,
          false,
          true)
    }
    if (name == "release" && descriptor == "()V") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.anonlab.fray.runtime.Runtime::onSemaphoreRelease,
              access,
              name,
              descriptor,
              true,
              true)
      return MethodExitVisitor(
          eMv,
          org.anonlab.fray.runtime.Runtime::onSemaphoreReleaseDone,
          access,
          name,
          descriptor,
          false,
          false,
          true)
    }
    if (name == "release" && descriptor == "(I)V") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.anonlab.fray.runtime.Runtime::onSemaphoreReleasePermits,
              access,
              name,
              descriptor,
              true,
              true)
      return MethodExitVisitor(
          eMv,
          org.anonlab.fray.runtime.Runtime::onSemaphoreReleaseDone,
          access,
          name,
          descriptor,
          false,
          false,
          true)
    }
    if (name == "drainPermits") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.anonlab.fray.runtime.Runtime::onSemaphoreDrainPermits,
              access,
              name,
              descriptor,
              true,
              false)
      return MethodExitVisitor(
          eMv,
          org.anonlab.fray.runtime.Runtime::onSemaphoreDrainPermitsDone,
          access,
          name,
          descriptor,
          false,
          false,
          true)
    }
    if (name == "reducePermits") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.anonlab.fray.runtime.Runtime::onSemaphoreReducePermits,
              access,
              name,
              descriptor,
              true,
              true)
      return MethodExitVisitor(
          eMv,
          org.anonlab.fray.runtime.Runtime::onSemaphoreReducePermitsDone,
          access,
          name,
          descriptor,
          false,
          false,
          true)
    }
    return mv
  }
}
