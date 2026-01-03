package org.pastalab.fray.instrumentation.base.visitors

import kotlin.reflect.KFunction
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ARETURN
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Opcodes.DRETURN
import org.objectweb.asm.Opcodes.FRETURN
import org.objectweb.asm.Opcodes.IRETURN
import org.objectweb.asm.Opcodes.LRETURN
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
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
    val thisType: String,
    val customizer: MethodExitVisitor.(v: MethodExitVisitor, isFinalBlock: Boolean) -> Unit =
        { v, isFinalBlock ->
        },
) : GeneratorAdapter(ASM9, mv, access, name, descriptor) {
  var methodEnterLabel = Label()
  var methodExitLabel = Label()
  var methodExitInsnCount = 0
  val tryCatchBlockLabelPairs = mutableListOf<Pair<Label, Label>>()

  override fun visitCode() {
    methodEnterLabel = mark()
    super.visitCode()
  }

  override fun visitIntInsn(opcode: Int, operand: Int) {
    super.visitIntInsn(opcode, operand)
    methodExitInsnCount++
  }

  override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
    super.visitFieldInsn(opcode, owner, name, descriptor)
    methodExitInsnCount++
  }

  override fun visitIincInsn(varIndex: Int, increment: Int) {
    super.visitIincInsn(varIndex, increment)
    methodExitInsnCount++
  }

  override fun visitJumpInsn(opcode: Int, label: Label?) {
    super.visitJumpInsn(opcode, label)
    methodExitInsnCount++
  }

  override fun visitLdcInsn(value: Any?) {
    super.visitLdcInsn(value)
    methodExitInsnCount++
  }

  override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
    super.visitMultiANewArrayInsn(descriptor, numDimensions)
    methodExitInsnCount++
  }

  override fun visitInvokeDynamicInsn(
      name: String?,
      descriptor: String?,
      bootstrapMethodHandle: Handle?,
      vararg bootstrapMethodArguments: Any?,
  ) {
    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
    methodExitInsnCount++
  }

  override fun visitMethodInsn(
      opcode: Int,
      owner: String?,
      name: String?,
      descriptor: String?,
      isInterface: Boolean,
  ) {
    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    methodExitInsnCount++
  }

  override fun visitVarInsn(opcode: Int, varIndex: Int) {
    super.visitVarInsn(opcode, varIndex)
    methodExitInsnCount++
  }

  override fun visitLookupSwitchInsn(dflt: Label?, keys: IntArray?, labels: Array<out Label?>?) {
    super.visitLookupSwitchInsn(dflt, keys, labels)
    methodExitInsnCount++
  }

  override fun visitTypeInsn(opcode: Int, type: String?) {
    super.visitTypeInsn(opcode, type)
    methodExitInsnCount++
  }

  override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label?, vararg labels: Label?) {
    super.visitTableSwitchInsn(min, max, dflt, *labels)
    methodExitInsnCount++
  }

  fun insertTryCatchBlock() {
    if (methodExitInsnCount > 0) {
      tryCatchBlockLabelPairs.add(Pair(methodEnterLabel, methodExitLabel))
    }
    methodEnterLabel = mark()
    methodExitInsnCount = 0
  }

  override fun visitInsn(opcode: Int) {
    when (opcode) {
      IRETURN,
      FRETURN,
      ARETURN,
      LRETURN,
      DRETURN,
      RETURN -> {
        methodExitLabel = mark()
        if (loadThis) {
          loadThis()
        }
        if (loadArgs) {
          loadArgs()
        }
        customizer(this, false)
        invokeStatic(
            Type.getObjectType(
                Runtime::class.java.name.replace(".", "/"),
            ),
            Utils.kFunctionToASMMethod(method),
        )
        super.visitInsn(opcode)
        if (addFinalBlock) {
          insertTryCatchBlock()
        }
        return
      }
    }
    super.visitInsn(opcode)
    methodExitInsnCount++
  }

  override fun visitMaxs(maxStack: Int, maxLocals: Int) {
    if (addFinalBlock) {
      methodExitLabel = mark()
      insertTryCatchBlock()
      if (tryCatchBlockLabelPairs.isNotEmpty()) {
        for ((start, end) in tryCatchBlockLabelPairs) {
          catchException(
              start,
              end,
              Type.getObjectType("java/lang/Throwable"),
          )
        }
        val locals = getLocals(thisType)
        visitFrame(Opcodes.F_NEW, locals.size, locals, 1, arrayOf("java/lang/Throwable"))
        if (loadThis) {
          loadThis()
        }
        if (loadArgs) {
          loadArgs()
        }
        customizer(this, true)
        invokeStatic(
            Type.getObjectType(
                Runtime::class.java.name.replace(".", "/"),
            ),
            Utils.kFunctionToASMMethod(method),
        )
        throwException()
      }
    }
    super.visitMaxs(maxStack, maxLocals)
  }
}
