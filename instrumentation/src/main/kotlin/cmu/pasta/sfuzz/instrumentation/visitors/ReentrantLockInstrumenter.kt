package cmu.pasta.sfuzz.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import java.util.concurrent.locks.ReentrantLock
import cmu.pasta.sfuzz.runtime.Runtime

class ReentrantLockInstrumenter(cv:ClassVisitor): ClassVisitorBase(cv, ReentrantLock::class.java.name) {

    override fun instrumentMethod(
        mv: MethodVisitor,
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (name == "tryLock") {
            return MethodEnterVisitor(mv, Runtime::onReentrantLockTryLock)
        }
        if (name == "lock" || name == " lockInterruptibly") {
            return MethodEnterVisitor(mv, Runtime::onReentrantLockLock)
        }
        if (name == "unlock") {
            return MethodEnterVisitor(mv, Runtime::onReentrantLockUnlock)
        }
        return mv
    }
}