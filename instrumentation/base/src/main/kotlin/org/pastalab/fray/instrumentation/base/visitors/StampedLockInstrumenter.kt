package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.pastalab.fray.runtime.Runtime

class StampedLockInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, "java/util/concurrent/locks/StampedLock") {

  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    val skipMv =
        MethodEnterVisitor(mv, Runtime::onStampedLockSkip, access, name, descriptor, false, false)
    if (name == "writeLock") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onStampedLockWriteLock,
              access,
              name,
              descriptor,
              true,
              false)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onStampedLockSkipDone,
          access,
          name,
          descriptor,
          false,
          false,
          false)
    }
    if (name == "readLock") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onStampedLockReadLock,
              access,
              name,
              descriptor,
              true,
              false)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onStampedLockSkipDone,
          access,
          name,
          descriptor,
          false,
          false,
          false)
    }
    if (name == "tryReadLock") {
      val method =
          if (descriptor == "()J") {
            Runtime::onStampedLockReadLockTryLock
          } else {
            Runtime::onStampedLockReadLockTryLockTimeout
          }
      val eMv =
          MethodEnterVisitor(
              mv,
              method,
              access,
              name,
              descriptor,
              true,
              true,
          ) {
            if (descriptor != "()J") {
              storeArg(0)
            }
          }
      return MethodExitVisitor(
          eMv, Runtime::onStampedLockSkipDone, access, name, descriptor, false, false, false)
    }
    if (name == "tryWriteLock") {
      val method =
          if (descriptor == "()J") {
            Runtime::onStampedLockWriteLockTryLock
          } else {
            Runtime::onStampedLockWriteLockTryLockTimeout
          }
      val eMv =
          MethodEnterVisitor(mv, method, access, name, descriptor, true, true) {
            if (descriptor != "()J") {
              storeArg(0)
            }
          }
      return MethodExitVisitor(
          eMv, Runtime::onStampedLockSkipDone, access, name, descriptor, false, false, false)
    }
    if (name == "readLockInterruptibly") {
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onStampedLockReadLockInterruptibly,
              access,
              name,
              descriptor,
              true,
              false)
      return MethodExitVisitor(
          eMv, Runtime::onStampedLockSkipDone, access, name, descriptor, false, false, false)
    }
    if (name == "writeLockInterruptibly") {
      return MethodExitVisitor(
          skipMv, Runtime::onStampedLockSkipDone, access, name, descriptor, false, false, false)
    }
    if (name == "unlockWrite") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockUnlockWriteDone,
          access,
          name,
          descriptor,
          true,
          false,
          false)
    }
    if (name == "unlockRead") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockUnlockReadDone,
          access,
          name,
          descriptor,
          true,
          false,
          false)
    }
    if (name == "tryConvertToWriteLock") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockTryConvertToWriteLockDone,
          access,
          name,
          descriptor,
          true,
          true,
          false) {
            dup2()
          }
    }
    if (name == "tryConvertToReadLock") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockTryConvertToReadLockDone,
          access,
          name,
          descriptor,
          true,
          true,
          false) {
            dup2()
          }
    }
    if (name == "tryConvertToOptimisticRead") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockTryConvertToOptimisticReadLockDone,
          access,
          name,
          descriptor,
          true,
          true,
          false) {
            dup2()
          }
    }
    if (name == "tryUnlockRead") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockTryUnlockReadDone,
          access,
          name,
          descriptor,
          true,
          false,
          false) {
            dup2()
          }
    }
    if (name == "tryUnlockWrite") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockTryUnlockWriteDone,
          access,
          name,
          descriptor,
          true,
          false,
          false) {
            dup2()
          }
    }
    return super.instrumentMethod(mv, access, name, descriptor, signature, exceptions)
  }
}
