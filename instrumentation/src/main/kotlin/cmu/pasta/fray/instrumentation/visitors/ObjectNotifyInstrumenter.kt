package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Opcodes.DUP

class ObjectNotifyInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  var className = ""

  override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?
  ) {
    className = name
    super.visit(version, access, name, signature, superName, interfaces)
  }

  override fun visitMethod(
      access: Int,
      name: String?,
      descriptor: String?,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    // TODO(aoli): we should make it more generic.
    if (className == "java/lang/ref/NativeReferenceQueue" ||
        className == "java/lang/Object" ||
        className.startsWith("jdk/internal") ||
        className.startsWith("java/lang/ref")) {
      // Let's skip this class because it does not guard `wait`
      // https://github.com/openjdk/jdk/blob/jdk-21-ga/src/java.base/share/classes/java/lang/ref/NativeReferenceQueue.java#L48
      return super.visitMethod(access, name, descriptor, signature, exceptions)
    }
    return object :
        MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
      override fun visitFrame(
          type: Int,
          numLocal: Int,
          local: Array<out Any>?,
          numStack: Int,
          stack: Array<out Any>?
      ) {
        super.visitFrame(type, numLocal, local, numStack, stack)
      }

      override fun visitMethodInsn(
          opcode: Int,
          owner: String?,
          callee: String?,
          descriptor: String?,
          isInterface: Boolean
      ) {
        if (callee == "notify" && descriptor == "()V") {
          super.visitInsn(DUP)
          super.visitMethodInsn(
              Opcodes.INVOKESTATIC,
              Runtime::class.java.name.replace(".", "/"),
              Runtime::onObjectNotify.name,
              Utils.kFunctionToJvmMethodDescriptor(Runtime::onObjectNotify),
              false)
          super.visitMethodInsn(opcode, owner, callee, descriptor, isInterface)
        } else if (callee == "notifyAll" && descriptor == "()V") {
          super.visitInsn(DUP)
          super.visitMethodInsn(
              Opcodes.INVOKESTATIC,
              Runtime::class.java.name.replace(".", "/"),
              Runtime::onObjectNotifyAll.name,
              Utils.kFunctionToJvmMethodDescriptor(Runtime::onObjectNotifyAll),
              false)
          super.visitMethodInsn(opcode, owner, callee, descriptor, isInterface)
        } else {
          super.visitMethodInsn(opcode, owner, callee, descriptor, isInterface)
        }
      }
    }
  }
}
