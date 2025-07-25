package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type

class ClassConstructorInstrumenter(cv: ClassVisitor, val isJDK: Boolean) : ClassVisitor(ASM9, cv) {
  var className = ""
  var clinitVisited = false
  var shouldInstrument = true

  override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String?>?
  ) {
    className = name
    if (isJDK && className.contains("internal")) {
      shouldInstrument = false
    }
    if (className == "java/lang/Module\$ReflectionData" ||
      className.startsWith("org/gradle/api/internal")) {
      shouldInstrument = false
    }
    super.visit(version, access, name, signature, superName, interfaces)
  }

  override fun visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    if (name == "<clinit>" && shouldInstrument) {
      clinitVisited = true
      return MethodExitVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onClassPrepareDone,
          access,
          name,
          descriptor,
          false,
          false,
          true,
          className,
          customizer = { mv, isFinalBlock -> push(Type.getObjectType(className)) })
    }
    return mv
  }

  override fun visitEnd() {
    if (!clinitVisited && shouldInstrument) {
      val mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)
      mv.visitCode()
      mv.visitLdcInsn(Type.getObjectType(className))
      mv.visitMethodInsn(
          Opcodes.INVOKESTATIC,
          "org/pastalab/fray/runtime/Runtime",
          "onClassPrepareDone",
          "(Ljava/lang/Class;)V",
          false)
      mv.visitInsn(Opcodes.RETURN)
      mv.visitEnd()
    }
    super.visitEnd()
  }
}
