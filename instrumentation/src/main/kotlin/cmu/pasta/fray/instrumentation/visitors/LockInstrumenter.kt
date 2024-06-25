package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock
import kotlin.reflect.KFunction
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
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
          MethodEnterVisitor(mv, Runtime::onLockTryLock, access, name, descriptor, true, false)
      return MethodExitVisitor(
          eMv, Runtime::onLockTryLockDone, access, name, descriptor, true, false, true)
    }
    if (name == "tryLock" && descriptor == "(JLjava/util/concurrent/TimeUnit;)Z") {
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onLockTryLockInterruptibly,
              access,
              name,
              descriptor,
              true,
              true,
              postCustomizer = { storeArg(0) })
      return MethodExitVisitor(
          eMv, Runtime::onLockTryLockInterruptiblyDone, access, name, descriptor, true, false, true)
    }
    if (name == "hasQueuedThreads") {
      return MethodExitVisitor(
          mv, Runtime::onLockHasQueuedThreads, access, name, descriptor, true, false, false)
    }
    if (name == "hasQueuedThread") {
      return MethodExitVisitor(
          mv, Runtime::onLockHasQueuedThread, access, name, descriptor, true, true, false)
    }
    if (name == "lock" || name == "lockInterruptibly") {
      val eMv =
          if (name == "lock") {
            MethodEnterVisitor(mv, Runtime::onLockLock, access, name, descriptor, true, false)
          } else {
            MethodEnterVisitor(
                mv, Runtime::onLockLockInterruptibly, access, name, descriptor, true, false)
          }
      return MethodExitVisitor(
          eMv, Runtime::onLockLockDone, access, name, descriptor, false, false, true)
    }
    if (name == "unlock") {
      val eMv = MethodEnterVisitor(mv, Runtime::onLockUnlock, access, name, descriptor, true, false)
      return MethodExitVisitor(
          eMv, Runtime::onLockUnlockDone, access, name, descriptor, true, false, true)
    }
    if (name == "newCondition") {
      return NewConditionVisitor(mv, Runtime::onLockNewCondition, access, name, descriptor)
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
            Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
            Utils.kFunctionToASMMethod(method))
        super.onMethodExit(opcode)
      }
    }
  }
}
