package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import kotlin.reflect.KFunction

class MethodEnterInstrumenter(mv: MethodVisitor, val method: KFunction<*>): MethodVisitor(ASM9, mv) {
    override fun visitCode() {
        super.visitCode()
        visitVarInsn(Opcodes.ALOAD, 0) // Load this
        visitMethodInsn(Opcodes.INVOKESTATIC,
            Runtime::class.java.name.replace(".", "/"), method.name,
            Utils.kFunctionToJvmMethodDescriptor(method), false)
    }
}