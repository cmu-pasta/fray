package cmu.pasta.sfuzz.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class ThreadInstrumenter(cv: ClassVisitor): ClassVisitorBase(cv, Thread::class.java.name) {

    override fun instrumentMethod(
        mv: MethodVisitor,
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (name == "start") {
            val eMv = MethodExitVisitor(mv, Runtime::onThreadStartDone, access, name, descriptor, true, false)
            return MethodEnterVisitor(eMv, Runtime::onThreadStart, access, name, descriptor, true, false)
        }
        if (name == "yield") {
            return MethodEnterVisitor(mv, Runtime::onYield, access, name, descriptor, false, false)
        }
        if (name == "getAndClearInterrupt") {
            return MethodExitVisitor(mv, Runtime::onThreadGetAndClearInterrupt, access, name, descriptor, true, false)
        }
        if (name == "clearInterrupt") {
            return MethodExitVisitor(mv, Runtime::onThreadClearInterrupt, access, name, descriptor, true, false)
        }
        if (name == "setInterrupt" || name == "interrupt") {
            return MethodEnterVisitor(mv, Runtime::onThreadInterrupt, access, name, descriptor, true, false)
        }
        return mv
    }
}