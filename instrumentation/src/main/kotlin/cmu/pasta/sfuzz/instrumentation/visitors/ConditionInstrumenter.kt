package cmu.pasta.sfuzz.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import cmu.pasta.sfuzz.runtime.Runtime
import java.util.concurrent.locks.AbstractQueuedSynchronizer.ConditionObject

class ConditionInstrumenter(cv:ClassVisitor): ClassVisitorBase(cv, ConditionObject::class.java.name) {

    override fun instrumentMethod(
        mv: MethodVisitor,
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (name.startsWith("await")) {
            val eMv = MethodEnterVisitor(mv, Runtime::onConditionAwait, access, name, descriptor, true, false)
            return MethodExitVisitor(eMv, Runtime::onConditionAwaitDone, access, name, descriptor, true, false)
        }
        if (name == "signal") {
            return MethodEnterVisitor(mv, Runtime::onConditionSignal, access, name, descriptor, true, false)
        }
        if (name == "signalAll") {
            return MethodEnterVisitor(mv, Runtime::onConditionSignalAll, access, name, descriptor, true, false)
        }
        return mv
    }
}
