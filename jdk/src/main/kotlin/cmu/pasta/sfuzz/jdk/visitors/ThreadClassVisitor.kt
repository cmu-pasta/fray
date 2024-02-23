package cmu.pasta.sfuzz.jdk.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import cmu.pasta.sfuzz.runtime.Runtime

class ThreadClassVisitor(cv: ClassVisitor): ClassVisitor(ASM9, cv) {
    var shouldInstrument = false

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String,
        superName: String,
        interfaces: Array<out String>
    ) {
        if (name == Thread::class.java.name) {
            shouldInstrument = true
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String,
        exceptions: Array<out String>
    ): MethodVisitor {
        if (shouldInstrument && METHOD_DELEGATE.containsKey(name)) {
            var delegate = METHOD_DELEGATE[name]!!
            return object: MethodVisitor(ASM9) {
                override fun visitCode() {
                    super.visitCode()
                    visitMethodInsn(Opcodes.INVOKESTATIC, Runtime.javaClass.name, delegate.name,
                        Utils.kFunctionToJvmMethodDescriptor(delegate), false)
                }
            }
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }

    companion object {
        val METHOD_DELEGATE = mapOf(
            Thread::start.name to Runtime::onThreadStart,
            Thread::run.name to Runtime::onThreadRun,
        )
    }
}