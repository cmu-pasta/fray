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
      exceptions: Array<out String>?,
  ): MethodVisitor {
    val skipMv =
        MethodEnterVisitor(
            mv,
            Runtime::onStampedLockSkip,
            access,
            name,
            descriptor,
            loadThis = false,
            loadArgs = false,
        )
    if (name == "writeLock") {
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onStampedLockWriteLock,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false,
          )
      return MethodExitVisitor(
          eMv,
          Runtime::onStampedLockSkipDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className,
      )
    }
    if (name == "readLock") {
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onStampedLockReadLock,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false,
          )
      return MethodExitVisitor(
          eMv,
          Runtime::onStampedLockSkipDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className,
      )
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
              loadThis = true,
              loadArgs = true,
          ) {
            if (descriptor != "()J") {
              storeArg(0)
            }
          }
      return MethodExitVisitor(
          eMv,
          Runtime::onStampedLockSkipDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className,
      )
    }
    if (name == "tryWriteLock") {
      val method =
          if (descriptor == "()J") {
            Runtime::onStampedLockWriteLockTryLock
          } else {
            Runtime::onStampedLockWriteLockTryLockTimeout
          }
      val eMv =
          MethodEnterVisitor(
              mv,
              method,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = true,
          ) {
            if (descriptor != "()J") {
              storeArg(0)
            }
          }
      return MethodExitVisitor(
          eMv,
          Runtime::onStampedLockSkipDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className,
      )
    }
    if (name == "readLockInterruptibly") {
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onStampedLockReadLockInterruptibly,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false,
          )
      return MethodExitVisitor(
          eMv,
          Runtime::onStampedLockSkipDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className,
      )
    }
    if (name == "writeLockInterruptibly") {
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onStampedLockWriteLockInterruptibly,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = false,
          )
      return MethodExitVisitor(
          eMv,
          Runtime::onStampedLockSkipDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className,
      )
    }
    if (name == "unlockWrite") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockUnlockWriteDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className,
      )
    }
    if (name == "unlockRead") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockUnlockReadDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className,
      )
    }
    if (name == "tryConvertToWriteLock") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockTryConvertToWriteLockDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = true,
          addFinalBlock = false,
          thisType = className,
      )
    }
    if (name == "tryConvertToReadLock") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockTryConvertToReadLockDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = true,
          addFinalBlock = false,
          thisType = className,
      )
    }
    if (name == "tryConvertToOptimisticRead") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockTryConvertToOptimisticReadLockDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = true,
          addFinalBlock = false,
          thisType = className,
      )
    }
    if (name == "tryUnlockRead") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockTryUnlockReadDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className,
      )
    }
    if (name == "tryUnlockWrite") {
      return MethodExitVisitor(
          skipMv,
          Runtime::onStampedLockTryUnlockWriteDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = false,
          thisType = className,
      )
    }
    return super.instrumentMethod(mv, access, name, descriptor, signature, exceptions)
  }
}
