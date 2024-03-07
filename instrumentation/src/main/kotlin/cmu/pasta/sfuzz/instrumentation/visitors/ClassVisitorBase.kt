package cmu.pasta.sfuzz.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.tree.MethodNode

open class ClassVisitorBase(cv:ClassVisitor, className: String): ClassVisitor(ASM9, cv) {
    var shouldInstrument = false
    var className: String

    init {
        this.className = className.replace(".", "/")
    }

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        if (name == className) {
            shouldInstrument = true
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }

    final override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return if (shouldInstrument) instrumentMethod(mv, access, name, descriptor, signature, exceptions)
        else mv
    }

    open fun instrumentMethod(
        mv: MethodVisitor,
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        return mv
    }
}