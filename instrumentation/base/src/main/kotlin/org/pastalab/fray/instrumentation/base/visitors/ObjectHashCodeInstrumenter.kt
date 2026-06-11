package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

class ObjectHashCodeInstrumenter(cv: ClassVisitor, val instrumentJdk: Boolean) :
    ClassVisitor(ASM9, cv) {
  var shouldInstrument = true

  override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?,
  ) {
    if (instrumentJdk && !(name == "java/util/Arrays" || name.startsWith("java/util/concurrent"))) {
      shouldInstrument = false
    }
    super.visit(version, access, name, signature, superName, interfaces)
  }

  override fun visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?,
  ): MethodVisitor {
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    if (!shouldInstrument) {
      return mv
    }
    return object : AdviceAdapter(ASM9, mv, access, name, descriptor) {
      override fun visitMethodInsn(
          opcodeAndSource: Int,
          owner: String,
          name: String,
          descriptor: String,
          isInterface: Boolean,
      ) {
        if (name == "hashCode" && owner == "java/lang/Object") {
          invokeStatic(
              Type.getObjectType(
                  org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/")
              ),
              Utils.kFunctionToASMMethod(org.pastalab.fray.runtime.Runtime::onObjectHashCode),
          )
        } else {
          super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
        }
      }
    }
  }
}
