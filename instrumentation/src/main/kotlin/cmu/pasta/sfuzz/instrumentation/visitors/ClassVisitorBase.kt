package cmu.pasta.sfuzz.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes.ASM9

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

}