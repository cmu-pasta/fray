package org.pastalab.fray.instrumentation.visitors

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock
import kotlin.reflect.KFunction
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

class LockInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(
        cv, ReentrantLock::class.java.name, ReadLock::class.java.name, WriteLock::class.java.name) {

  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "tryLock" && descriptor == "()Z") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onLockTryLock,
              access,
              name,
              descriptor,
              true,
              false)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onLockTryLockDone,
          access,
          name,
          descriptor,
          true,
          false,
          true)
    }
    if (name == "tryLock" && descriptor == "(JLjava/util/concurrent/TimeUnit;)Z") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onLockTryLockInterruptibly,
              access,
              name,
              descriptor,
              true,
              true,
              postCustomizer = { storeArg(0) })
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onLockTryLockInterruptiblyDone,
          access,
          name,
          descriptor,
          true,
          false,
          true)
    }
    if (name == "hasQueuedThreads") {
      return MethodExitVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onLockHasQueuedThreads,
          access,
          name,
          descriptor,
          true,
          false,
          false)
    }
    if (name == "hasQueuedThread") {
      return MethodExitVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onLockHasQueuedThread,
          access,
          name,
          descriptor,
          true,
          true,
          false)
    }
    if (name == "lock" || name == "lockInterruptibly") {
      val eMv =
          if (name == "lock") {
            MethodEnterVisitor(
                mv,
                org.pastalab.fray.runtime.Runtime::onLockLock,
                access,
                name,
                descriptor,
                true,
                false)
          } else {
            MethodEnterVisitor(
                mv,
                org.pastalab.fray.runtime.Runtime::onLockLockInterruptibly,
                access,
                name,
                descriptor,
                true,
                false)
          }
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onLockLockDone,
          access,
          name,
          descriptor,
          false,
          false,
          true)
    }
    if (name == "unlock") {
      val eMv =
          MethodEnterVisitor(
              mv,
              org.pastalab.fray.runtime.Runtime::onLockUnlock,
              access,
              name,
              descriptor,
              true,
              false)
      return MethodExitVisitor(
          eMv,
          org.pastalab.fray.runtime.Runtime::onLockUnlockDone,
          access,
          name,
          descriptor,
          true,
          false,
          true)
    }
    if (name == "newCondition") {
      return NewConditionVisitor(
          mv, org.pastalab.fray.runtime.Runtime::onLockNewCondition, access, name, descriptor)
    }
    return mv
  }

  class NewConditionVisitor(
      mv: MethodVisitor,
      val method: KFunction<*>,
      access: Int,
      name: String,
      descriptor: String
  ) : AdviceAdapter(ASM9, mv, access, name, descriptor) {
    override fun onMethodExit(opcode: Int) {
      if (opcode == ARETURN) {
        dup()
        loadThis()
        invokeStatic(
            Type.getObjectType(
                org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/")),
            Utils.kFunctionToASMMethod(method))
        super.onMethodExit(opcode)
      }
    }
  }
}
