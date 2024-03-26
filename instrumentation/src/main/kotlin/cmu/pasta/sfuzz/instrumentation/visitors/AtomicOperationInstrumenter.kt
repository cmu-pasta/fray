package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.MemoryOpType
import cmu.pasta.sfuzz.runtime.Runtime
import java.util.*
import java.util.concurrent.atomic.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*

class AtomicOperationInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
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
      name: String,
      descriptor: String?,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    val memoryType = memoryTypeFromMethodName(name)
    if (atomicClasses.contains(className) &&
        !atomicNonVolatileMethodNames.contains(name) &&
        access and ACC_PUBLIC != 0) {
      return object : MethodVisitor(ASM9, mv) {
        override fun visitCode() {
          super.visitCode()
          val type = MemoryOpType::class.java.name.replace(".", "/")
          visitVarInsn(ALOAD, 0)
          visitFieldInsn(GETSTATIC, type, memoryType.name, "L$type;")
          visitMethodInsn(
              INVOKESTATIC,
              cmu.pasta.sfuzz.runtime.Runtime::class.java.name.replace(".", "/"),
              Runtime::onAtomicOperation.name,
              Utils.kFunctionToJvmMethodDescriptor(Runtime::onAtomicOperation),
              false)
        }
      }
    }
    return mv
  }

  fun memoryTypeFromMethodName(name: String): MemoryOpType {
    val lname = name.lowercase()
    return if (lname.contains("set") || lname.contains("exchange")) {
      MemoryOpType.MEMORY_WRITE
    } else {
      MemoryOpType.MEMORY_READ
    }
  }

  companion object {
    val atomicClasses: List<String> =
        Arrays.asList(
            AtomicBoolean::class.java.name.replace('.', '/'),
            AtomicInteger::class.java.name.replace('.', '/'),
            AtomicIntegerArray::class.java.name.replace('.', '/'),
            AtomicIntegerFieldUpdater::class.java.name.replace('.', '/'),
            AtomicLong::class.java.name.replace('.', '/'),
            AtomicLongArray::class.java.name.replace('.', '/'),
            AtomicLongFieldUpdater::class.java.name.replace('.', '/'),
            AtomicMarkableReference::class.java.name.replace('.', '/'),
            AtomicReference::class.java.name.replace('.', '/'),
            AtomicReferenceArray::class.java.name.replace('.', '/'),
            AtomicReferenceFieldUpdater::class.java.name.replace('.', '/'),
            AtomicStampedReference::class.java.name.replace('.', '/'),
            DoubleAccumulator::class.java.name.replace('.', '/'),
            DoubleAdder::class.java.name.replace('.', '/'),
            LongAccumulator::class.java.name.replace('.', '/'),
            LongAdder::class.java.name.replace('.', '/'))
    val atomicNonVolatileMethodNames: List<String> =
        mutableListOf(
            "<init>",
            "<clinit>",
            "getPlain",
            "setPlain",
            "toString",
            "weakCompareAndSetPlain",
            "length",
            "hashcode",
            "equals",
            "clone",
            "getClass")
  }
}
