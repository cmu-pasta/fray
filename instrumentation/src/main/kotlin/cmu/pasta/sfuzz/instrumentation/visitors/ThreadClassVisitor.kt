package cmu.pasta.sfuzz.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import cmu.pasta.sfuzz.runtime.Runtime

class ThreadClassVisitor(cv: ClassVisitor): ClassVisitorBase(cv, Thread::class.java.name) {
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        var mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (shouldInstrument && name == "start") {
            return MethodEnterInstrumenter(MethodExitInstrumenter(mv, Runtime::onThreadStartDone), Runtime::onThreadStart)
        }
        return mv
    }
}