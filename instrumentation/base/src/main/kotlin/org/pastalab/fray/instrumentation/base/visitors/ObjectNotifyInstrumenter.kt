package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9

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
    //    if (className == "java/lang/ref/NativeReferenceQueue" ||
    //        className == "java/lang/Object" ||
    //        className.startsWith("jdk/internal") ||
    //        className.startsWith("java/lang/ref")) {
    //      // Let's skip this class because it does not guard `wait`
    //      //
    // https://github.com/openjdk/jdk/blob/jdk-21-ga/src/java.base/share/classes/java/lang/ref/NativeReferenceQueue.java#L48
    //      return super.visitMethod(access, name, descriptor, signature, exceptions)
    //    }
    if (className.startsWith("java") &&
        (!className.startsWith("java/util/concurrent") || (className != "java/lang/Thread")) ||
        access and Opcodes.ACC_NATIVE != 0) {
      return super.visitMethod(access, name, descriptor, signature, exceptions)
    }
    if (className.startsWith("jdk") ||
        className.startsWith("apple") ||
        className.startsWith("com/sun") ||
        className.startsWith("com/apple") ||
        className.startsWith("org/w3c") ||
        className.startsWith("sun")) {
      return super.visitMethod(access, name, descriptor, signature, exceptions)
    }
    return object :
        MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
      override fun visitMethodInsn(
          opcode: Int,
          owner: String?,
          callee: String?,
          descriptor: String?,
          isInterface: Boolean
      ) {
        if (callee == "notify" && descriptor == "()V") {
          super.visitMethodInsn(
              Opcodes.INVOKESTATIC,
              org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
              org.pastalab.fray.runtime.Runtime::OBJECTNOTIFY.name,
              Utils.kFunctionToJvmMethodDescriptor(org.pastalab.fray.runtime.Runtime::OBJECTNOTIFY),
              false)
        } else if (callee == "notifyAll" && descriptor == "()V") {
          super.visitMethodInsn(
              Opcodes.INVOKESTATIC,
              org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
              org.pastalab.fray.runtime.Runtime::OBJECTNOTIFYALL.name,
              Utils.kFunctionToJvmMethodDescriptor(
                  org.pastalab.fray.runtime.Runtime::OBJECTNOTIFYALL),
              false)
        } else if (callee == "wait" && descriptor == "(J)V") {
          super.visitMethodInsn(
              Opcodes.INVOKESTATIC,
              org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
              org.pastalab.fray.runtime.Runtime::OBJECTWAIT.name,
              Utils.kFunctionToJvmMethodDescriptor(org.pastalab.fray.runtime.Runtime::OBJECTWAIT),
              false)
        } else if (callee == "wait" && descriptor == "()V") {
          super.visitLdcInsn(0L)
          super.visitMethodInsn(
              Opcodes.INVOKESTATIC,
              org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/"),
              org.pastalab.fray.runtime.Runtime::OBJECTWAIT.name,
              Utils.kFunctionToJvmMethodDescriptor(org.pastalab.fray.runtime.Runtime::OBJECTWAIT),
              false)
        } else {
          super.visitMethodInsn(opcode, owner, callee, descriptor, isInterface)
        }
      }
    }
  }
}
