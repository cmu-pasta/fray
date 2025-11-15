package org.pastalab.fray.instrumentation.base.visitors

import java.util.concurrent.ForkJoinPool
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.tree.MethodNode

// This is a super hacky way of handling blocking inside ForkJoinPool.
// Fray assumes no additional operation happens when a thread is blocked
// through `Condition.await` or `Lock.lock`. However, this assumption does not
// hold for ForkJoinPool's managed blocking, which can create a new thread
// to compensate for the blocked thread.
// https://github.com/openjdk/jdk/blob/f510b4a3bafa3f0d2c9ebf0b33d48f57f3bdef95/src/java.base/share/classes/java/util/concurrent/ForkJoinPool.java#L4303
// This can break many things. For example,
// https://github.com/cmu-pasta/fray/blob/c4d89a74c88c4cebe194740e926cf867df46d037/core/src/main/kotlin/org/pastalab/fray/core/RunContext.kt#L466
// will wait for the curren thread to be blocked then schedule a new thread.
// If the current thread creates a new thread after it is blocked. Fray may have
// ConcurrentModificationException or miss scheduling some threads.
// Currently, we solve this problem by changing the `managedBlock` method to
// only call `unmanagedBlock(blocker);`.
class ForkJoinPoolManagedBlockInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, ForkJoinPool::class.java.name) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "managedBlock" &&
        descriptor == $$"(Ljava/util/concurrent/ForkJoinPool$ManagedBlocker;)V") {
      mv.visitCode()
      mv.visitVarInsn(ALOAD, 0)
      mv.visitMethodInsn(
          INVOKESTATIC,
          "java/util/concurrent/ForkJoinPool",
          "unmanagedBlock",
          $$"(Ljava/util/concurrent/ForkJoinPool$ManagedBlocker;)V",
          false)
      mv.visitInsn(RETURN)
      mv.visitEnd()
      return MethodNode()
    }
    return mv
  }
}
