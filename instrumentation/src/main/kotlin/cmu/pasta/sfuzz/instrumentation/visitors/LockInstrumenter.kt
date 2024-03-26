package cmu.pasta.sfuzz.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import java.util.concurrent.locks.ReentrantLock
import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock
import kotlin.reflect.KFunction

class LockInstrumenter(cv:ClassVisitor): ClassVisitorBase(cv, ReentrantLock::class.java.name, ReadLock::class.java.name,
    WriteLock::class.java.name) {

    override fun instrumentMethod(
        mv: MethodVisitor,
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (name == "tryLock") {
            return MethodEnterVisitor(mv, Runtime::onLockTryLock, access, name, descriptor, true, false)
        }
        if (name == "lock" || name == "lockInterruptibly") {
            val eMv = MethodEnterVisitor(mv, Runtime::onLockLock, access, name, descriptor, true, false)
            return MethodExitVisitor(eMv, Runtime::onLockLockDone, access, name, descriptor, true, false)
        }
        if (name == "unlock") {
            val eMv = MethodEnterVisitor(mv, Runtime::onLockUnlock, access, name, descriptor, true, false)
            return MethodExitVisitor(eMv, Runtime::onLockUnlockDone, access, name, descriptor, true, false)
        }
        if (name == "newCondition") {
            return NewConditionVisitor(mv, Runtime::onLockNewCondition, access, name, descriptor)
        }
        return mv
    }

    class NewConditionVisitor(mv: MethodVisitor, val method: KFunction<*>, access: Int, name: String, descriptor: String): AdviceAdapter(ASM9, mv, access, name, descriptor) {
        override fun onMethodExit(opcode: Int) {
            if (opcode == ARETURN) {
                dup()
                loadThis()
                visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    Runtime::class.java.name.replace(".", "/"), method.name,
                    Utils.kFunctionToJvmMethodDescriptor(method), false
                )
                super.onMethodExit(opcode)
            }
        }
    }
}