package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class ObjectInstrumenter(cv: ClassVisitor): ClassVisitorBase(cv, Object::class.java.name) {
    override fun instrumentMethod(
        mv: MethodVisitor,
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (name == "wait" && descriptor == "(J)V") {
            return MethodExitVisitor(MethodEnterVisitor(mv, Runtime::onObjectWait, true),
                Runtime::onObjectWaitDone, access, name, descriptor, true)
        }
        return super.instrumentMethod(mv, access, name, descriptor, signature, exceptions)
    }
}