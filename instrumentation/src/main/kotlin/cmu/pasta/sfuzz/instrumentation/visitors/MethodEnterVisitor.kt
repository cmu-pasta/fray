package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import kotlin.reflect.KFunction

class MethodEnterVisitor(mv: MethodVisitor, val method: KFunction<*>, access: Int, name: String, descriptor: String,
                         val loadThis: Boolean, val loadArgs: Boolean): AdviceAdapter(ASM9, mv, access, name, descriptor) {
    override fun visitCode() {
        super.visitCode()
        if (loadThis) {
            loadThis()
        }
        if (loadArgs) {
            loadArgs()
        }
        visitMethodInsn(Opcodes.INVOKESTATIC,
            Runtime::class.java.name.replace(".", "/"), method.name,
            Utils.kFunctionToJvmMethodDescriptor(method), false)
    }
}