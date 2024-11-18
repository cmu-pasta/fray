package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.tree.MethodNode

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
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    if (access and Opcodes.ACC_NATIVE != 0 || access and Opcodes.ACC_SYNCHRONIZED == 0) {
      return mv
    }
    return object : MethodNode(ASM9, access, name, descriptor, signature, exceptions) {
      var shouldInstrument = true

      override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
        if (descriptor == "Ljdk/internal/vm/annotation/IntrinsicCandidate;") {
          shouldInstrument = false
        }
        return super.visitAnnotation(descriptor, visible)
      }

      override fun visitEnd() {
        super.visitEnd()
        if (!shouldInstrument) {
          accept(mv)
        } else {
          val newAccess = access and Opcodes.ACC_SYNCHRONIZED.inv()
          val newMv =
              object : AdviceAdapter(ASM9, mv, newAccess, name, descriptor) {
                val enterLabel = Label()

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
                  val label = mark()
                  catchException(enterLabel, label, Type.getObjectType("java/lang/Throwable"))
                  val locals = getLocals()
                  visitFrame(
                      Opcodes.F_NEW, locals.size, getLocals(), 1, arrayOf("java/lang/Throwable"))
                  if (access and Opcodes.ACC_STATIC != 0) {
                    push(Type.getObjectType(className))
                  } else {
                    loadThis()
                  }
                  visitInsn(MONITOREXIT)
                  visitInsn(ATHROW)
                  super.visitMaxs(maxStack, maxLocals)
                }
              }
          accept(newMv)
        }
      }
    }
  }
}
