package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class ObjectVisitor(cv: ClassVisitor): ClassVisitorBase(cv, Object::class.java.name) {
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        var mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (shouldInstrument && name == "wait" && descriptor == "(L)V") {
            return MethodExitInstrumenter(MethodEnterInstrumenter(mv, Runtime::onObjectWait), Runtime::onObjectWaitDone)
        }
        return mv
    }
}