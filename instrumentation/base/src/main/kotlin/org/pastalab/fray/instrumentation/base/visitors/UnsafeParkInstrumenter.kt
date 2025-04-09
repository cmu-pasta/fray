package org.pastalab.fray.instrumentation.base.visitors

import java.util.concurrent.ForkJoinPool
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.pastalab.fray.runtime.Runtime

// As for JDK 24, the `Usafe.park` is only used in `ForkJoinPool` and let's only
// instrument `ForkJoinPool`. For other use cases, we have already instrumented
// its higher level APIs, such as `LockSupport` and `Condition`.
class UnsafeParkInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, ForkJoinPool::class.java.name) {

  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    return object : GeneratorAdapter(ASM9, mv, access, name, descriptor) {
      override fun visitMethodInsn(
          opcode: Int,
          owner: String,
          name: String,
          descriptor: String,
          isInterface: Boolean
      ) {
        if (owner == "jdk/internal/misc/Unsafe") {
          if (name == "park") {
            invokeStatic(
                Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
                Utils.kFunctionToASMMethod(Runtime::onUnsafeThreadParkTimed))
            // Pop the Unsafe instance
            pop()
            return
          } else if (name == "unpark") {
            dup()
            invokeStatic(
                Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
                Utils.kFunctionToASMMethod(Runtime::onThreadUnpark))
            dup()
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            invokeStatic(
                Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
                Utils.kFunctionToASMMethod(Runtime::onThreadUnparkDone))
            return
          }
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      }
    }
  }
}
