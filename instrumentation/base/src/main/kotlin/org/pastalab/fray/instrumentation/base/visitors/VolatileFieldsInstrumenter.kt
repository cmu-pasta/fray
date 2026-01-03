package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.commons.AdviceAdapter
import org.pastalab.fray.instrumentation.base.memory.VolatileManager

class VolatileFieldsInstrumenter(
    cv: ClassVisitor,
    private val instrumentingJdk: Boolean,
    private val interleaveAllMemoryOps: Boolean,
) : ClassVisitor(ASM9, cv) {
  var className = ""
  var shouldInstrument = !instrumentingJdk

  override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?,
  ) {
    super.visit(version, access, name, signature, superName, interfaces)
    className = name
    shouldInstrument = !instrumentingJdk || name.startsWith("java/util/concurrent")
  }

  override fun visitField(
      access: Int,
      name: String,
      descriptor: String?,
      signature: String?,
      value: Any?,
  ): FieldVisitor {
    volatileManager.setVolatile(className, name, access)
    return super.visitField(access, name, descriptor, signature, value)
  }

  fun recursiveVisitClass(ownerName: String): Boolean {
    return ownerName.startsWith(className) && ownerName != className
  }

  override fun visitMethod(
      access: Int,
      name: String?,
      descriptor: String?,
      signature: String?,
      exceptions: Array<out String>?,
  ): MethodVisitor {
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    if (name == "<init>" || name == "<clinit>" || !shouldInstrument) {
      return mv
    }
    return object : AdviceAdapter(ASM9, mv, access, name, descriptor) {
      override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        if (
            (recursiveVisitClass(owner) ||
                volatileManager.isVolatile(owner, name) ||
                interleaveAllMemoryOps) && !owner.startsWith("org/pastalab/fray/runtime/")
        ) {

          if (opcode == GETFIELD) {
            dup()
          }
          if (opcode == PUTFIELD) {
            if (descriptor == "J" || descriptor == "D") {
              dup2X1() // value, objectref, value
              pop2() // value, objectref
            } else {
              swap()
            }
            dup() // value, objectref, objectref
          }
          visitLdcInsn(owner)
          visitLdcInsn(name)
          visitLdcInsn(descriptor)
          when (opcode) {
            GETFIELD ->
                super.visitMethodInsn(
                    INVOKESTATIC,
                    org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
                    org.pastalab.fray.runtime.Runtime::onFieldRead.name,
                    Utils.kFunctionToJvmMethodDescriptor(
                        org.pastalab.fray.runtime.Runtime::onFieldRead
                    ),
                    false,
                )
            PUTFIELD ->
                super.visitMethodInsn(
                    INVOKESTATIC,
                    org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
                    org.pastalab.fray.runtime.Runtime::onFieldWrite.name,
                    Utils.kFunctionToJvmMethodDescriptor(
                        org.pastalab.fray.runtime.Runtime::onFieldWrite
                    ),
                    false,
                )
            PUTSTATIC ->
                super.visitMethodInsn(
                    INVOKESTATIC,
                    org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
                    org.pastalab.fray.runtime.Runtime::onStaticFieldWrite.name,
                    Utils.kFunctionToJvmMethodDescriptor(
                        org.pastalab.fray.runtime.Runtime::onStaticFieldWrite
                    ),
                    false,
                )
            GETSTATIC ->
                super.visitMethodInsn(
                    INVOKESTATIC,
                    org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
                    org.pastalab.fray.runtime.Runtime::onStaticFieldRead.name,
                    Utils.kFunctionToJvmMethodDescriptor(
                        org.pastalab.fray.runtime.Runtime::onStaticFieldRead
                    ),
                    false,
                )
          }
          if (opcode == PUTFIELD) {
            if (descriptor == "J" || descriptor == "D") {
              dupX2()
              pop()
            } else {
              swap() // objectref, value
            }
          }
        }
        super.visitFieldInsn(opcode, owner, name, descriptor)
      }
    }
  }

  companion object {
    val volatileManager = VolatileManager(false)
  }
}
