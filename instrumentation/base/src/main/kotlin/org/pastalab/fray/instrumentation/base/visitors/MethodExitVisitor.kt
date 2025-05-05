package org.pastalab.fray.instrumentation.base.visitors

import kotlin.reflect.KFunction
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.pastalab.fray.runtime.Runtime

class MethodExitVisitor(
    mv: MethodVisitor,
    val method: KFunction<*>,
    access: Int,
    name: String,
    descriptor: String,
    val loadThis: Boolean,
    val loadArgs: Boolean,
    val addFinalBlock: Boolean,
    val customizer: MethodExitVisitor.(v: MethodExitVisitor, isFinalBlock: Boolean) -> Unit =
        { v, isFinalBlock ->
        }
) : AdviceAdapter(ASM9, mv, access, name, descriptor) {
  val methodEnterLabel = Label()
  val methodExitLabel = Label()

  override fun visitCode() {
    visitLabel(methodEnterLabel)
    super.visitCode()
  }

  fun insertMethodExitBlock(isFinalBlock: Boolean) {
    if (loadThis) {
      loadThis()
    }
    if (loadArgs) {
      loadArgs()
    }
    customizer(this, isFinalBlock)
    invokeStatic(
        Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
        Utils.kFunctionToASMMethod(method),
    )
  }

  override fun onMethodExit(opcode: Int) {
    if (opcode != ATHROW) {
      insertMethodExitBlock(false)
    }
    super.onMethodExit(opcode)
  }

  override fun visitMaxs(maxStack: Int, maxLocals: Int) {
    if (addFinalBlock) {
      visitLabel(methodExitLabel)
      catchException(methodEnterLabel, methodExitLabel, null)
      val locals = getLocals()
      visitFrame(Opcodes.F_NEW, locals.size, locals, 1, arrayOf("java/lang/Throwable"))
      insertMethodExitBlock(true)
      throwException()
    }
    super.visitMaxs(maxStack, maxLocals)
  }
}
