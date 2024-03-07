package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class ClassloaderInstrumenter(cv: ClassVisitor): ClassVisitorBase(cv, ClassLoader::class.java.name) {
    override fun instrumentMethod(
        mv: MethodVisitor,
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (name == "loadClass" && descriptor == "(Ljava/lang/String;)Ljava/lang/Class;") {
            return MethodExitVisitor(MethodEnterVisitor(mv, Runtime::onLoadClass, false), Runtime::onLoadClassDone, access, name, descriptor, false)
        }
        return super.instrumentMethod(mv, access, name, descriptor, signature, exceptions)
    }
}