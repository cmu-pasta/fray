package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method

class ArrayOperationInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  override fun visitMethod(
      access: Int,
      name: String?,
      descriptor: String?,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    return object : GeneratorAdapter(ASM9, mv, access, name, descriptor) {
      override fun visitInsn(opcode: Int) {
        if (opcode == AALOAD ||
            opcode == BALOAD ||
            opcode == CALOAD ||
            opcode == DALOAD ||
            opcode == FALOAD ||
            opcode == IALOAD ||
            opcode == LALOAD ||
            opcode == SALOAD) {
          dup2()
          invokeStatic(
              Type.getObjectType(
                  org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/")),
              Method(
                  org.pastalab.fray.runtime.Runtime::onArrayLoad.name,
                  Utils.kFunctionToJvmMethodDescriptor(
                      org.pastalab.fray.runtime.Runtime::onArrayLoad)),
          )
        }
        if (opcode == AASTORE ||
            opcode == BASTORE ||
            opcode == CASTORE ||
            opcode == DASTORE ||
            opcode == FASTORE ||
            opcode == IASTORE ||
            opcode == LASTORE ||
            opcode == SASTORE) {
          if (opcode == LASTORE || opcode == DASTORE) {
            dup2X2() // value, arrayref, index, value,
            pop2() // value, arrayref, index
          } else {
            dupX2() // value, arrayref, index, value
            pop() // value, arrayref, index
          }
          dup2() // value, arrayref, index, arrayref, index
          invokeStatic(
              Type.getObjectType(
                  org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/")),
              Utils.kFunctionToASMMethod(org.pastalab.fray.runtime.Runtime::onArrayStore),
          )
          /*
           Now we have [value, arrayref, index], we need to
           restore the stack to [arrayref, index, value]
          */
          if (opcode == LASTORE || opcode == DASTORE) {
            dup2X2() // arrayref, index, value, arrayref, index
            pop2() // value, arrayref, index
          } else {
            dup2X1() // arrayref, index, value, arrayref, index
            pop2() // arrayref, index, value
          }
        }
        super.visitInsn(opcode)
      }
    }
  }
}
