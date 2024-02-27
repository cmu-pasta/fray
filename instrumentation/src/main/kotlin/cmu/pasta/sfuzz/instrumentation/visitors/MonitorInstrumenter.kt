package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9

class MonitorInstrumenter(cv: ClassVisitor): ClassVisitor(ASM9, cv) {
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
            className.startsWith("java/net/URL") || className.startsWith("jdk/internal/ref")) {
            // Let's skip this class because it does not guard `wait`
            // https://github.com/openjdk/jdk/blob/jdk-21-ga/src/java.base/share/classes/java/lang/ref/NativeReferenceQueue.java#L48
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }

        var mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return object: MethodVisitor(ASM9, mv) {
            override fun visitInsn(opcode: Int) {
                if (opcode == Opcodes.MONITORENTER) {
                    super.visitInsn(Opcodes.DUP)
                    super.visitMethodInsn(Opcodes.INVOKESTATIC,
                        Runtime::class.java.name.replace(".", "/"),
                        Runtime::onReentrantLockLock.name,
                        Utils.kFunctionToJvmMethodDescriptor(Runtime::onReentrantLockLock),
                        false)
                } else if (opcode == Opcodes.MONITOREXIT) {
                    super.visitInsn(Opcodes.DUP)
                    super.visitMethodInsn(Opcodes.INVOKESTATIC,
                        Runtime::class.java.name.replace(".", "/"),
                        Runtime::onReentrantLockUnlock.name,
                        Utils.kFunctionToJvmMethodDescriptor(Runtime::onReentrantLockUnlock),
                        false)
                }
                super.visitInsn(opcode)
            }
        }
    }
}