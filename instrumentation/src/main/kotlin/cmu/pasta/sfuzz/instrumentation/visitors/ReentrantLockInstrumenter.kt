package cmu.pasta.sfuzz.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import java.util.concurrent.locks.ReentrantLock
import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.commons.AdviceAdapter
import kotlin.reflect.KFunction

class ReentrantLockInstrumenter(cv:ClassVisitor): ClassVisitorBase(cv, ReentrantLock::class.java.name) {

    override fun instrumentMethod(
        mv: MethodVisitor,
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (name == "tryLock") {
            return MethodEnterVisitor(mv, Runtime::onReentrantLockTryLock, true)
        }
        if (name == "lock" || name == " lockInterruptibly") {
            return MethodEnterVisitor(mv, Runtime::onReentrantLockLock, true)
        }
        if (name == "unlock") {
            return MethodExitVisitor(MethodEnterVisitor(mv, Runtime::onReentrantLockUnlock, true),
                Runtime::onReentrantLockUnlockDone, access, name, descriptor, true)
        }
        if (name == "newCondition") {
            return NewConditionVisitor(mv, Runtime::onReentrantLockNewCondition, access, name, descriptor, true)
        }
        return mv
    }

    class NewConditionVisitor(mv: MethodVisitor, val method: KFunction<*>, access: Int, name: String, descriptor: String, val loadThis: Boolean = true): AdviceAdapter(ASM9, mv, access, name, descriptor) {
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