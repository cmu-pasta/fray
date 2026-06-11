package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ReentrantReadWriteLockInstrumenter(cv: ClassVisitor, className: String) :
    ClassVisitorBase(cv, className) {

  override fun visitEnd() {
    if (shouldInstrument) {
      super.visitField(Opcodes.ACC_PUBLIC, RW_LOCK_FIELD_NAME, RW_LOCK_FIELD_TYPE, null, null)
    }
    super.visitEnd()
  }

  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?,
  ): MethodVisitor {
    if (name == "<init>" && descriptor == "(${RW_LOCK_FIELD_TYPE})V") {
      return MethodExitVisitor(
          mv,
          null,
          access,
          name,
          descriptor,
          loadThis = true,
          loadArgs = true,
          addFinalBlock = false,
          thisType = className,
      ) { v, isFinalBlock ->
        v.visitFieldInsn(Opcodes.PUTFIELD, className, RW_LOCK_FIELD_NAME, RW_LOCK_FIELD_TYPE)
      }
    }
    return mv
  }

  companion object {
    const val RW_LOCK_FIELD_NAME = "__fray\$RWLock"
    const val RW_LOCK_FIELD_TYPE = "Ljava/util/concurrent/locks/ReentrantReadWriteLock;"
  }
}
