package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*

class SystemModulesMapInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(cv, "jdk.internal.module.SystemModulesMap") {

  class ReturnNullMV(val mmv: MethodVisitor) : MethodVisitor(ASM9) {
    override fun visitCode() {
      mmv.visitCode()
      mmv.visitInsn(ACONST_NULL)
      mmv.visitInsn(ARETURN)
      mmv.visitEnd()
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
      mmv.visitMaxs(1, maxLocals)
    }
  }

  class ReturnEmptyArrayMV(val mmv: MethodVisitor) : MethodVisitor(ASM9) {
    override fun visitCode() {
      mmv.visitCode()
      mmv.visitInsn(ICONST_0)
      mmv.visitTypeInsn(ANEWARRAY, "java/lang/String")
      mmv.visitInsn(ARETURN)
      mmv.visitEnd()
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
      mmv.visitMaxs(1, maxLocals)
    }
  }

  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?,
  ): MethodVisitor {
    if (name == "allSystemModules" || name == "defaultSystemModules") {
      return ReturnNullMV(mv)
    }
    if (name == "moduleNames" || name == "classNames") {
      return ReturnEmptyArrayMV(mv)
    }
    return mv
  }
}
