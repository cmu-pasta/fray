package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter
import java.util.concurrent.locks.LockSupport

class LockSupportInstrumenter(cv: ClassVisitor): ClassVisitorBase(cv, LockSupport::class.java.name) {
    override fun instrumentMethod(
        mv: MethodVisitor,
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        var mv = super.instrumentMethod(mv, access, name, descriptor, signature, exceptions)
        if (name.startsWith("park")) {
            return MethodExitVisitor(MethodEnterVisitor(mv, Runtime::onThreadPark, false), Runtime::onThreadParkDone, access, name, descriptor, false)
        }
        if (name.startsWith("unpark")) {
            return object: AdviceAdapter(ASM9, mv, access, name, descriptor) {
                override fun onMethodEnter() {
                    loadArg(0)
                    mv.visitMethodInsn(INVOKESTATIC,
                        Runtime::class.java.name.replace('.', '/'),
                        Runtime::onThreadUnpark.name,
                        Utils.kFunctionToJvmMethodDescriptor(Runtime::onThreadUnpark),
                        false)
                    super.onMethodEnter()
                }

                override fun onMethodExit(opcode: Int) {
                    loadArg(0)
                    mv.visitMethodInsn(INVOKESTATIC,
                        Runtime::class.java.name.replace('.', '/'),
                        Runtime::onThreadUnparkDone.name,
                        Utils.kFunctionToJvmMethodDescriptor(Runtime::onThreadUnparkDone),
                        false)
                    super.onMethodExit(opcode)
                }
            }
        }
        return mv
    }
    
}