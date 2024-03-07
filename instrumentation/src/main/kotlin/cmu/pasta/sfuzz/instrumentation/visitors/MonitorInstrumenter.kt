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
        var mv = if (access and Opcodes.ACC_SYNCHRONIZED != 0 && !instrumentingJDK) {
            val newMethod = "$name\$SFuzz"
            var newAccess = access and Opcodes.ACC_SYNCHRONIZED.inv()

            var copyMv = super.visitMethod(newAccess, name, descriptor, signature, exceptions)

            copyMv.visitCode()
            var (opcode, argIndex) = if (access and Opcodes.ACC_STATIC != 0) {
                copyMv.visitLdcInsn(Type.getObjectType(className))
                Pair(Opcodes.INVOKESTATIC, 0)
            } else {
                copyMv.visitVarInsn(Opcodes.ALOAD, 0) // Load this
                copyMv.visitInsn(Opcodes.DUP)
                Pair(Opcodes.INVOKEVIRTUAL, 1)
            }
            copyMv.visitMethodInsn(Opcodes.INVOKESTATIC,
                Runtime::class.java.name.replace(".", "/"),
                Runtime::onReentrantLockLock.name,
                Utils.kFunctionToJvmMethodDescriptor(Runtime::onReentrantLockLock),
                false)
//            copyMv.visitInsn(Opcodes.MONITORENTER)
            val args: Array<Type> = Type.getArgumentTypes(descriptor)
            for (i in args.indices) {
                val t = args[i]
                copyMv.visitVarInsn(t.getOpcode(Opcodes.ILOAD), argIndex)
                argIndex += t.size
            }
            copyMv.visitMethodInsn(opcode, className, newMethod, descriptor, false)
            if (access and Opcodes.ACC_STATIC != 0) {
                copyMv.visitLdcInsn(Type.getObjectType(className))
            } else {
                copyMv.visitVarInsn(Opcodes.ALOAD, 0) // Load this
            }
            copyMv.visitMethodInsn(Opcodes.INVOKESTATIC,
                Runtime::class.java.name.replace(".", "/"),
                Runtime::onReentrantLockUnlock.name,
                Utils.kFunctionToJvmMethodDescriptor(Runtime::onReentrantLockUnlock),
                false)
//            copyMv.visitInsn(Opcodes.MONITOREXIT)

            val returnType = Type.getReturnType(descriptor)
            copyMv.visitInsn(returnType.getOpcode(Opcodes.IRETURN))
            copyMv.visitMaxs(args.size, args.size);
            copyMv.visitEnd()
            SeparationMethodVisitor(
                super.visitMethod(newAccess, "$name\$SFuzz", descriptor, signature, exceptions), copyMv)
        } else {
            super.visitMethod(access, name, descriptor, signature, exceptions)
        }

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