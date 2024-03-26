package cmu.pasta.sfuzz.instrumentation.visitors

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.commons.AdviceAdapter

class SynchronizedMethodInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  var className = ""

  override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?
  ) {
    super.visit(version, access, name, signature, superName, interfaces)
    className = name
  }

  override fun visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {

    if (access and Opcodes.ACC_NATIVE != 0 || access and Opcodes.ACC_SYNCHRONIZED == 0) {
      return super.visitMethod(access, name, descriptor, signature, exceptions)
    }
    val newAccess = access and Opcodes.ACC_SYNCHRONIZED.inv()
    val mv = super.visitMethod(newAccess, name, descriptor, signature, exceptions)
    return object : AdviceAdapter(ASM9, mv, newAccess, name, descriptor) {
      val enterLabel = Label()
      val exitLabel = Label()

      override fun onMethodEnter() {
        super.onMethodEnter()
        visitLabel(enterLabel)
        if (access and Opcodes.ACC_STATIC != 0) {
          push(Type.getObjectType(className))
        } else {
          loadThis()
        }
        visitInsn(MONITORENTER)
      }

      override fun onMethodExit(opcode: Int) {
        if (opcode != ATHROW) {
          if (access and Opcodes.ACC_STATIC != 0) {
            push(Type.getObjectType(className))
          } else {
            loadThis()
          }
          visitInsn(MONITOREXIT)
        }
        super.onMethodExit(opcode)
      }

      override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        visitTryCatchBlock(enterLabel, exitLabel, exitLabel, "java/lang/Throwable")
        visitLabel(exitLabel)
        if (access and Opcodes.ACC_STATIC != 0) {
          visitFrame(F_NEW, 0, arrayOf<Any>(), 1, arrayOf<Any>("java/lang/Throwable"))
          push(Type.getObjectType(className))
        } else {
          visitFrame(F_NEW, 1, arrayOf<Any>(className), 1, arrayOf<Any>("java/lang/Throwable"))
          loadThis()
        }
        visitInsn(MONITOREXIT)
        visitInsn(ATHROW)
        super.visitMaxs(maxStack, maxLocals)
      }
    }
  }
}
