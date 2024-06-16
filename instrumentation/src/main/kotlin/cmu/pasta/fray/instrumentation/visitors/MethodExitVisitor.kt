package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
import kotlin.reflect.KFunction
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

class MethodExitVisitor(
    mv: MethodVisitor,
    val method: KFunction<*>,
    access: Int,
    name: String,
    descriptor: String,
    val loadThis: Boolean,
    val loadArgs: Boolean,
    val addFinalBlock: Boolean,
    val customizer: MethodExitVisitor.(v: MethodExitVisitor) -> Unit = {}
) : AdviceAdapter(ASM9, mv, access, name, descriptor) {
  val methodEnterLabel = Label()
  val methodExitLabel = Label()

  override fun visitCode() {
    visitLabel(methodEnterLabel)
    super.visitCode()
  }

  override fun onMethodExit(opcode: Int) {
    if (opcode != ATHROW) {
      if (loadThis) {
        loadThis()
      }
      if (loadArgs) {
        loadArgs()
      }
      customizer(this)
      invokeStatic(
          Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
          Utils.kFunctionToASMMethod(method),
      )
    }
    super.onMethodExit(opcode)
  }

  override fun visitMaxs(maxStack: Int, maxLocals: Int) {
    if (addFinalBlock) {
      visitLabel(methodExitLabel)
      catchException(methodEnterLabel, methodExitLabel, null)
      val locals = getLocals()
      visitFrame(Opcodes.F_NEW, locals.size, getLocals(), 1, arrayOf("java/lang/Throwable"))
      if (loadThis) {
        loadThis()
      }
      if (loadArgs) {
        loadArgs()
      }
      customizer(this)
      invokeStatic(
          Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
          Utils.kFunctionToASMMethod(method),
      )
      throwException()
    }
    super.visitMaxs(maxStack, maxLocals)
  }
}
