package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.pastalab.fray.runtime.Runtime

class SleepInstrumenter(cv: ClassVisitor, val isJDK: Boolean) : ClassVisitor(ASM9, cv) {

  var shouldInstrument = !isJDK

  override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String?>?,
  ) {
    super.visit(version, access, name, signature, superName, interfaces)
    if (isJDK && name.startsWith("java/util/concurrent/")) {
      shouldInstrument = true
    }
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
    return object : GeneratorAdapter(ASM9, mv, access, name, descriptor) {
      override fun visitMethodInsn(
          opcode: Int,
          owner: String,
          name: String,
          descriptor: String,
          isInterface: Boolean,
      ) {
        if (owner == "java/lang/Thread" && name == "sleep") {
          when (descriptor) {
            "(J)V" -> {
              invokeStatic(
                  Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
                  Utils.kFunctionToASMMethod(Runtime::onThreadSleepMillis),
              )
            }
            "(JI)V" -> {
              invokeStatic(
                  Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
                  Utils.kFunctionToASMMethod(Runtime::onThreadSleepMillisNanos),
              )
            }
            else -> {
              invokeStatic(
                  Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
                  Utils.kFunctionToASMMethod(Runtime::onThreadSleepDuration),
              )
            }
          }
        } else {
          super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
      }
    }
  }
}
