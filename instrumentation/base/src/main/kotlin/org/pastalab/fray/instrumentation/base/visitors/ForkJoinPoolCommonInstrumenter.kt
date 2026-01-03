package org.pastalab.fray.instrumentation.base.visitors

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.pastalab.fray.runtime.Runtime

class ForkJoinPoolCommonInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(
        cv,
        ForkJoinPool::class.java.name,
        ForkJoinTask::class.java.name,
        CompletableFuture::class.java.name,
    ) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?,
  ): MethodVisitor {
    return object : AdviceAdapter(ASM9, mv, access, name, descriptor) {
      override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String?) {
        super.visitFieldInsn(opcode, owner, name, descriptor)
        if (owner == ForkJoinPool::class.java.name.replace(".", "/")) {
          if (opcode == GETSTATIC && name == "common") {
            invokeStatic(
                Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
                Utils.kFunctionToASMMethod(Runtime::onForkJoinPoolCommonPool),
            )
            return
          }
        }
        if (owner == CompletableFuture::class.java.name.replace(".", "/")) {
          if (opcode == GETSTATIC && name == "ASYNC_POOL") {
            invokeStatic(
                Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
                Utils.kFunctionToASMMethod(Runtime::onForkJoinPoolCommonPool),
            )
            return
          }
        }
      }
    }
  }
}
