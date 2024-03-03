package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.TargetTerminateException
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.tree.MethodNode

class TargetExitInstrumenter(cv: ClassVisitor): ClassVisitorBase(cv, System::class.java.name) {
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (name == "exit") {
            mv.visitCode()
            mv.visitTypeInsn(Opcodes.NEW, TargetTerminateException::class.java.name.replace(".", "/"))
            mv.visitInsn(Opcodes.DUP)
            mv.visitLdcInsn(1)
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, TargetTerminateException::class.java.name.replace(".", "/"),
                "<init>", "(I)V", false)
            mv.visitInsn(Opcodes.ATHROW)
            return MethodNode()
        } else {
            return mv
        }
    }
}