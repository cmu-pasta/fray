package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.ASM9

class MonitorInstrumenter(cv: ClassVisitor, private val instrumentingJDK: Boolean): ClassVisitor(ASM9, cv) {
    var className = ""
    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        if (className.startsWith("jdk/internal/loader/") || className.startsWith("java/util/zip/") ||
            className.startsWith("java/net/URL") || className.startsWith("jdk/internal/ref") ||
            (className.startsWith("java/lang") && className != "java/lang/Thread") ||
            access and Opcodes.ACC_NATIVE != 0) {
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return object: MethodVisitor(ASM9, mv) {
            override fun visitInsn(opcode: Int) {
                if (opcode == Opcodes.MONITORENTER || opcode == Opcodes.MONITOREXIT) {
                    if (instrumentingJDK) super.visitInsn(Opcodes.DUP)
                    if (opcode == Opcodes.MONITORENTER) {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC,
                            Runtime::class.java.name.replace(".", "/"),
                            Runtime::onReentrantLockLock.name,
                            Utils.kFunctionToJvmMethodDescriptor(Runtime::onReentrantLockLock),
                            false)

                    } else {
                        super.visitMethodInsn(Opcodes.INVOKESTATIC,
                            Runtime::class.java.name.replace(".", "/"),
                            Runtime::onReentrantLockUnlock.name,
                            Utils.kFunctionToJvmMethodDescriptor(Runtime::onReentrantLockUnlock),
                            false)
                    }
                    if (instrumentingJDK) super.visitInsn(opcode)
                } else {
                    super.visitInsn(opcode)
                }
            }
        }
    }
}