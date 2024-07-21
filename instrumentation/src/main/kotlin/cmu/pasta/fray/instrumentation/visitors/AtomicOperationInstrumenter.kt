package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.MemoryOpType
import cmu.pasta.fray.runtime.Runtime
import java.util.*
import java.util.concurrent.atomic.*
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

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
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    return object : AdviceAdapter(ASM9, mv, access, name, descriptor) {
      override fun visitMethodInsn(
          opcodeAndSource: Int,
          owner: String,
          name: String,
          descriptor: String?,
          isInterface: Boolean
      ) {
        if (atomicClasses.contains(owner) && !atomicNonVolatileMethodNames.contains(name)) {

          val argumentTypes = Type.getArgumentTypes(descriptor)
          val paramArrayIndex = newLocal(Type.getType("[Ljava/lang/Object;"))
          push(argumentTypes.size)
          newArray(Type.getObjectType("java/lang/Object"))
          storeLocal(paramArrayIndex)
          for (i in argumentTypes.indices) { // store call parameters to an array
            val type = argumentTypes[argumentTypes.size - 1 - i]
            box(type)
            loadLocal(paramArrayIndex)
            swap()
            push(i)
            swap()
            arrayStore(Type.getObjectType("java/lang/Object"))
          }
          dup()
          val memoryType = memoryTypeFromMethodName(name)
          getStatic(
              Type.getType(MemoryOpType::class.java),
              memoryType.name,
              Type.getType(MemoryOpType::class.java))
          invokeStatic(
              Type.getType(Runtime::class.java),
              Utils.kFunctionToASMMethod(Runtime::onAtomicOperation))
          for (i in argumentTypes.indices.reversed()) { // load call parameters from an array
            loadLocal(paramArrayIndex)
            push(i)
            arrayLoad(Type.getObjectType("java/lang/Object"))
            unbox(argumentTypes[argumentTypes.size - 1 - i])
          }
        }
        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
      }
    }
  }

  fun memoryTypeFromMethodName(name: String): MemoryOpType {
    val lname = name.lowercase()
    return if (lname.contains("set") || lname.contains("exchange") || lname.contains("update")) {
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
