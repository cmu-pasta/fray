package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import kotlin.reflect.KFunction
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter

class MethodExitVisitor(
    mv: MethodVisitor,
    val method: KFunction<*>,
    access: Int,
    name: String,
    descriptor: String,
    val loadThis: Boolean,
    val loadArgs: Boolean
) : AdviceAdapter(ASM9, mv, access, name, descriptor) {
  val methodEnterLabel = Label()
  val methodExitLabel = Label()

  override fun onMethodEnter() {
    super.onMethodEnter()
    visitLabel(methodEnterLabel)
  }

  override fun onMethodExit(opcode: Int) {
    if (opcode != ATHROW) {
      if (loadThis) {
        loadThis()
      }
      if (loadArgs) {
        loadArgs()
      }
      visitMethodInsn(
          Opcodes.INVOKESTATIC,
          Runtime::class.java.name.replace(".", "/"),
          method.name,
          Utils.kFunctionToJvmMethodDescriptor(method),
          false)
    }
    super.onMethodExit(opcode)
  }

  override fun visitMaxs(maxStack: Int, maxLocals: Int) {
    visitLabel(methodExitLabel)
    visitTryCatchBlock(methodEnterLabel, methodExitLabel, methodExitLabel, "java/lang/Throwable")
    if (loadThis) {
      loadThis()
    }
    if (loadArgs) {
      loadArgs()
    }
    visitMethodInsn(
        Opcodes.INVOKESTATIC,
        Runtime::class.java.name.replace(".", "/"),
        method.name,
        Utils.kFunctionToJvmMethodDescriptor(method),
        false)
    visitInsn(ATHROW)
    super.visitMaxs(maxStack, maxLocals)
  }
}
