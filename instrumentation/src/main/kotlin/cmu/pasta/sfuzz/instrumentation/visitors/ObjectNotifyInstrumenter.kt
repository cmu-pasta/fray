package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9

class ObjectNotifyInstrumenter(cv: ClassVisitor): ClassVisitor(ASM9, cv) {
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        return object: MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
            override fun visitMethodInsn(
                opcode: Int,
                owner: String?,
                name: String?,
                descriptor: String?,
                isInterface: Boolean
            ) {
                if (name == "notify" && descriptor == "()V") {
                    super.visitInsn(Opcodes.DUP);
                    super.visitInsn(Opcodes.DUP);
                    super.visitMethodInsn(Opcodes.INVOKESTATIC,
                        Runtime::class.java.name.replace(".", "/"),
                        Runtime::onObjectNotify.name,
                        Utils.kFunctionToJvmMethodDescriptor(Runtime::onObjectNotify),
                        false)
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                    super.visitMethodInsn(Opcodes.INVOKESTATIC,
                        Runtime::class.java.name.replace(".", "/"),
                        Runtime::onObjectNotifyDone.name,
                        Utils.kFunctionToJvmMethodDescriptor(Runtime::onObjectNotifyDone),
                        false)
                } else {
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                }
            }
        }
    }
}