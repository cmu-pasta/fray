package cmu.pasta.sfuzz.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9

class ClassVersionInstrumenter(cv: ClassVisitor): ClassVisitor(ASM9, cv) {
    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        val patchedVersion = if (version < Opcodes.V1_5) {
            Opcodes.V1_5
        } else {
            version
        }
        super.visit(patchedVersion, access, name, signature, superName, interfaces)
    }
}