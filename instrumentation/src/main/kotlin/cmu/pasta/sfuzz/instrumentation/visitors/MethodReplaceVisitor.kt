package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import kotlin.reflect.KFunction

class MethodReplaceVisitor(mv: MethodVisitor, val method: KFunction<*>): MethodVisitor(ASM9, mv) {
    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        super.visitMethodInsn(Opcodes.INVOKESTATIC,
            Runtime::class.java.name.replace(".", "/"), method.name,
            Utils.kFunctionToJvmMethodDescriptor(method), false)
    }
}
