package cmu.pasta.sfuzz.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import cmu.pasta.sfuzz.runtime.Runtime

class ThreadInstrumenter(cv: ClassVisitor): ClassVisitorBase(cv, Thread::class.java.name) {

    override fun instrumentMethod(
        mv: MethodVisitor,
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (name == "start") {
            return MethodEnterVisitor(MethodExitVisitor(mv, Runtime::onThreadStartDone), Runtime::onThreadStart)
        }
        if (name == "yield") {
            return MethodEnterVisitor(mv, Runtime::onYield)
        }
        return mv
    }
}