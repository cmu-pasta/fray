package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.MemoryOpType
import cmu.pasta.fray.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Opcodes.GETSTATIC
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.commons.GeneratorAdapter

class AtomicGetInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  override fun visitMethod(
      access: Int,
      name: String?,
      descriptor: String?,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    return object : GeneratorAdapter(ASM9, mv, access, name, descriptor) {
      override fun visitMethodInsn(
          opcode: Int,
          owner: String,
          name: String,
          descriptor: String,
          isInterface: Boolean
      ) {
        if (AtomicOperationInstrumenter.atomicClasses.contains(owner) &&
            name == "get" &&
            descriptor.startsWith("()")) {
          dup()
          val type = MemoryOpType::class.java.name.replace(".", "/")
          visitFieldInsn(GETSTATIC, type, MemoryOpType.MEMORY_READ.name, "L$type;")
          visitMethodInsn(
              INVOKESTATIC,
              cmu.pasta.fray.runtime.Runtime::class.java.name.replace(".", "/"),
              Runtime::onAtomicOperation.name,
              Utils.kFunctionToJvmMethodDescriptor(Runtime::onAtomicOperation),
              false)
        }
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
      }
    }
  }
}
