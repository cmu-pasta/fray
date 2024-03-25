package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class ObjectInstrumenter(cv: ClassVisitor): ClassVisitorBase(cv, Object::class.java.name) {
    override fun instrumentMethod(
        mv: MethodVisitor,
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (name == "wait" && descriptor == "(J)V") {
            val eMv = MethodEnterVisitor(mv, Runtime::onObjectWait, access, name, descriptor, true, false)
            // We cannot use MethodExitVisitor here because `onObjectWaitDone` may throw an `InterruptedException`
            // So we cannot catch that exception twice.
            return object: AdviceAdapter(ASM9, eMv, access, name, descriptor) {
                val methodEnterLabel = Label()
                val methodExitLabel = Label()
                override fun onMethodEnter() {
                    super.onMethodEnter()
                    visitLabel(methodEnterLabel)
                }

                override fun onMethodExit(opcode: Int) {
                    if (opcode != ATHROW) {
                        visitLabel(methodExitLabel)
                        loadThis()
                        visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            Runtime::class.java.name.replace(".", "/"), Runtime::onObjectWaitDone.name,
                            Utils.kFunctionToJvmMethodDescriptor(Runtime::onObjectWaitDone), false)
                    }
                    super.onMethodExit(opcode)
                }

                override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                    val label = Label()
                    visitTryCatchBlock(methodEnterLabel, methodExitLabel, label, "java/lang/Throwable")
                    visitLabel(label)
                    loadThis()
                    visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        Runtime::class.java.name.replace(".", "/"), Runtime::onObjectWaitDone.name,
                        Utils.kFunctionToJvmMethodDescriptor(Runtime::onObjectWaitDone), false)
                    visitInsn(ATHROW)
                    super.visitMaxs(maxStack, maxLocals)
                }
            }
        }
        return mv
    }
}