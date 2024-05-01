package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
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
          mv, Runtime::onSemaphoreInit, access, name, descriptor, true, false, false)
    }
    if ((name == "acquire" || name == "acquireUninterruptibly") && descriptor == "()V") {
      val eMv =
          if (name == "acquire") {
            MethodEnterVisitor(
                mv, Runtime::onSemaphoreAcquire, access, name, descriptor, true, true)
          } else {
            MethodEnterVisitor(
                mv,
                Runtime::onSemaphoreAcquireUninterruptibly,
                access,
                name,
                descriptor,
                true,
                true)
          }
      return MethodExitVisitor(
          eMv, Runtime::onSemaphoreAcquireDone, access, name, descriptor, false, false, true)
    }
    if ((name == "acquire" || name == "acquireUninterruptibly") && descriptor == "(I)V") {
      val eMv =
          if (name == "acquire") {
            MethodEnterVisitor(
                mv, Runtime::onSemaphoreAcquirePermits, access, name, descriptor, true, true)
          } else {
            MethodEnterVisitor(
                mv,
                Runtime::onSemaphoreAcquirePermitsUninterruptibly,
                access,
                name,
                descriptor,
                true,
                true)
          }
      return MethodExitVisitor(
          eMv, Runtime::onSemaphoreAcquireDone, access, name, descriptor, false, false, true)
    }
    if (name == "release" && descriptor == "()V") {
      val eMv =
          MethodEnterVisitor(mv, Runtime::onSemaphoreRelease, access, name, descriptor, true, true)
      return MethodExitVisitor(
          eMv, Runtime::onSemaphoreReleaseDone, access, name, descriptor, false, false, true)
    }
    if (name == "release" && descriptor == "(I)V") {
      val eMv =
          MethodEnterVisitor(
              mv, Runtime::onSemaphoreReleasePermits, access, name, descriptor, true, true)
      return MethodExitVisitor(
          eMv, Runtime::onSemaphoreReleaseDone, access, name, descriptor, false, false, true)
    }
    if (name == "drainPermits") {
      val eMv =
          MethodEnterVisitor(
              mv, Runtime::onSemaphoreDrainPermits, access, name, descriptor, true, false)
      return MethodExitVisitor(
          eMv, Runtime::onSemaphoreDrainPermitsDone, access, name, descriptor, false, false, true)
    }
    if (name == "reducePermits") {
      val eMv =
          MethodEnterVisitor(
              mv, Runtime::onSemaphoreReducePermits, access, name, descriptor, true, true)
      return MethodExitVisitor(
          eMv, Runtime::onSemaphoreReducePermitsDone, access, name, descriptor, false, false, true)
    }
    return mv
  }
}