package cmu.pasta.sfuzz.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import java.util.concurrent.locks.ReentrantLock
import cmu.pasta.sfuzz.runtime.Runtime

class ReentrantLockInstrumenter(cv:ClassVisitor): ClassVisitorBase(cv, ReentrantLock::class.java.name) {
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        var mv = super.visitMethod(access, name, descriptor, signature, exceptions)

        if (!shouldInstrument) return mv

        if (name == "tryLock") {
            return MethodEnterInstrumenter(mv, Runtime::onReentrantLockTryLock)
        }
        if (name == "lock" || name == " lockInterruptibly") {
            return MethodEnterInstrumenter(mv, Runtime::onReentrantLockLock)
        }
        return mv
    }
}