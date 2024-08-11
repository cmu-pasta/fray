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
          true,
          false,
          false)
    }
    if ((name == "acquire" || name == "acquireUninterruptibly" || name == "tryAcquire") &&
        descriptor.startsWith("()")) {
      val eMv =
          if (name == "acquire") {
            MethodEnterVisitor(
                mv,
                org.pastalab.fray.runtime.Runtime::onSemaphoreAcquire,
                access,
                name,
                descriptor,
                true,
                true)
          } else if (name == "acquireUninterruptibly") {
            MethodEnterVisitor(
                mv,
                org.pastalab.fray.runtime.Runtime::onSemaphoreAcquireUninterruptibly,
                access,
                name,
                descriptor,
                true,
                true)
          } else {
            MethodEnterVisitor(
                mv,
                org.pastalab.fray.runtime.Runtime::onSemaphoreTryAcquire,
                access,
                name,
                descriptor,
                true,
                true)
          }
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSemaphoreAcquireDone,
          access,
          name,
          descriptor,
          false,
          false,
          true)
    }
    if ((name == "acquire" || name == "acquireUninterruptibly" || name == "tryAcquire") &&
        descriptor.startsWith("(I)V")) {
      val eMv =
          if (name == "acquire") {
            MethodEnterVisitor(
                mv,
                org.pastalab.fray.runtime.Runtime::onSemaphoreAcquirePermits,
                access,
                name,
                descriptor,
                true,
                true)
          } else if (name == "acquireUninterruptibly") {
            MethodEnterVisitor(
                mv,
                org.pastalab.fray.runtime.Runtime::onSemaphoreAcquirePermitsUninterruptibly,
                access,
                name,
                descriptor,
                true,
                true)
          } else {
            MethodEnterVisitor(
                mv,
                org.pastalab.fray.runtime.Runtime::onSemaphoreTryAcquirePermits,
                access,
                name,
                descriptor,
                true,
                true)
          }

      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSemaphoreAcquireDone,
          access,
          name,
          descriptor,
          false,
          false,
          true)
    }
    //    if (name == "tryAcquire") {
    //    }
    if (name == "release" && descriptor == "()V") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onSemaphoreRelease,
              access,
              name,
              descriptor,
              true,
              true)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSemaphoreReleaseDone,
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
              org.pastalab.fray.runtime.Runtime::onSemaphoreReleasePermits,
              access,
              name,
              descriptor,
              true,
              true)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSemaphoreReleaseDone,
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
              org.pastalab.fray.runtime.Runtime::onSemaphoreDrainPermits,
              access,
              name,
              descriptor,
              true,
              false)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSemaphoreDrainPermitsDone,
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
              org.pastalab.fray.runtime.Runtime::onSemaphoreReducePermits,
              access,
              name,
              descriptor,
              true,
              true)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onSemaphoreReducePermitsDone,
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
