package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Opcodes.ATHROW
import org.objectweb.asm.commons.AdviceAdapter
import kotlin.reflect.KFunction

class MethodExitVisitor(mv: MethodVisitor, val method: KFunction<*>, access: Int, name: String, descriptor: String, val loadThis: Boolean): AdviceAdapter(ASM9, mv, access, name, descriptor) {
    val methodEnterLabel = Label()
    val methodExitLabel = Label()
    override fun onMethodEnter() {
        super.onMethodEnter()
        visitLabel(methodEnterLabel)
    }

    override fun onMethodExit(opcode: Int) {
        if (loadThis) {
            loadThis()
        }
        if (opcode != ATHROW) {
            visitMethodInsn(Opcodes.INVOKESTATIC,
                Runtime::class.java.name.replace(".", "/"), method.name,
                Utils.kFunctionToJvmMethodDescriptor(method), false)
        }
        super.onMethodExit(opcode)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        visitTryCatchBlock(methodEnterLabel, methodExitLabel, methodExitLabel, "java/lang/Throwable")
        visitLabel(methodExitLabel)
        visitMethodInsn(Opcodes.INVOKESTATIC,
            Runtime::class.java.name.replace(".", "/"), method.name,
            Utils.kFunctionToJvmMethodDescriptor(method), false)
        visitInsn(ATHROW)
        super.visitMaxs(maxStack, maxLocals)
    }
}