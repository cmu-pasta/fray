package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.instrumentation.memory.MemoryManager
import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9

class VolatileFieldsInstrumenter(cv: ClassVisitor): ClassVisitor(ASM9, cv) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        var mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return object: MethodVisitor(ASM9, mv) {
            override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
                if (memoryManager.isVolatile(owner, name)) {
                    if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
                        visitVarInsn(Opcodes.ALOAD, 0)
                    }
                    visitLdcInsn(owner)
                    visitLdcInsn(name)
                    visitLdcInsn(descriptor)
                    when (opcode) {
                        Opcodes.GETFIELD -> super.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            Runtime::class.java.name.replace(".", "/"),
                            Runtime::onFieldRead.name,
                            Utils.kFunctionToJvmMethodDescriptor(Runtime::onFieldRead), false)
                        Opcodes.PUTFIELD -> super.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            Runtime::class.java.name.replace(".", "/"),
                            Runtime::onFieldWrite.name,
                            Utils.kFunctionToJvmMethodDescriptor(Runtime::onFieldWrite), false)
                        Opcodes.PUTSTATIC -> super.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            Runtime::class.java.name.replace(".", "/"),
                            Runtime::onStaticFieldWrite.name,
                            Utils.kFunctionToJvmMethodDescriptor(Runtime::onStaticFieldWrite), false)
                        Opcodes.GETSTATIC -> super.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            Runtime::class.java.name.replace(".", "/"),
                            Runtime::onStaticFieldRead.name,
                            Utils.kFunctionToJvmMethodDescriptor(Runtime::onStaticFieldRead), false)
                    }
                }
                super.visitFieldInsn(opcode, owner, name, descriptor)
            }
        }
    }

    companion object {
        val memoryManager = MemoryManager()
    }

}