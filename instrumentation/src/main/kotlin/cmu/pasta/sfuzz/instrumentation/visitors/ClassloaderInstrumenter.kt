package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9

class ClassloaderInstrumenter(cv: ClassVisitor): ClassVisitor(ASM9, cv) {
    var className: String = ""

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        if ((name == "loadClass" && descriptor == "(Ljava/lang/String;)Ljava/lang/Class;" && className == "java/lang/ClassLoader") ||
            (name == "makeImpl" && className == "java/lang/invoke/MethodType")) {
            return MethodExitVisitor(MethodEnterVisitor(mv, Runtime::onLoadClass, false), Runtime::onLoadClassDone, access, name, descriptor, false)
        }
        return mv
    }
}