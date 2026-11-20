package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.pastalab.fray.runtime.Runtime

class ObjectInstrumenter(cv: ClassVisitor) : ClassVisitorBase(cv, Object::class.java.name) {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?,
  ): MethodVisitor {
    if (name == "wait" && descriptor == "(J)V") {
      val eMv =
          MethodEnterVisitor(
              Wait0TimeoutVisitor(mv),
              Runtime::onObjectWait,
              access,
              name,
              descriptor,
              loadThis = true,
              loadArgs = true,
          )
      return MethodExitVisitor(
          eMv,
          Runtime::onObjectWaitDone,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className,
      )
    }
    return mv
  }

  /**
   * Passes the timeout of every native `wait0(long)` call in `Object.wait(long)` through
   * [Runtime.onObjectWait0Timeout], which returns 0 for a wait Fray models, so the JVM blocks it
   * untimed and Fray's timed-block bookkeeping delivers the timeout, as it already does for timed
   * park and timed `Condition.await`. `wait()`, `wait(long, int)` and, on platform threads,
   * `Thread.join(long)` all reach this method. Fails the instrumentation (and so the jlink build)
   * if the JDK's `Object.wait(long)` has no `wait0(long)` call to rewrite. Regression test:
   * `TimedObjectWaitStaysScheduledTest`.
   */
  private class Wait0TimeoutVisitor(mv: MethodVisitor) : MethodVisitor(Opcodes.ASM9, mv) {
    private var rewritten = 0

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean,
    ) {
      if (owner == OBJECT && name == "wait0" && descriptor == "(J)V") {
        // Stack: this, timeoutMillis -> this, timeout for the JVM.
        super.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            RUNTIME,
            HOOK.name,
            HOOK.descriptor,
            false,
        )
        rewritten++
      }
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitEnd() {
      check(rewritten > 0) {
        "Object.wait(long) has no wait0(long) call: cannot make Fray-blocked timed waits untimed"
      }
      super.visitEnd()
    }

    companion object {
      private val OBJECT = Type.getInternalName(Any::class.java)
      private val RUNTIME = Type.getInternalName(Runtime::class.java)
      private val HOOK = Utils.kFunctionToASMMethod(Runtime::onObjectWait0Timeout)
    }
  }
}
