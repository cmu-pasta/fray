package org.pastalab.fray.instrumentation.base.visitors

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray
import java.util.concurrent.atomic.AtomicLongFieldUpdater
import java.util.concurrent.atomic.AtomicMarkableReference
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.concurrent.atomic.AtomicStampedReference
import java.util.concurrent.atomic.DoubleAccumulator
import java.util.concurrent.atomic.DoubleAdder
import java.util.concurrent.atomic.LongAccumulator
import java.util.concurrent.atomic.LongAdder
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Opcodes.GETSTATIC
import org.pastalab.fray.runtime.Runtime

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
    val memoryType = memoryTypeFromMethodName(name)
    if (atomicClasses.contains(className) &&
        !atomicNonVolatileMethodNames.contains(name) &&
        access and ACC_PUBLIC != 0) {
      val type = org.pastalab.fray.runtime.MemoryOpType::class.java.name.replace(".", "/")
      val eMv =
          MethodEnterVisitor(
              mv,
              Runtime::onAtomicOperation,
              access,
              name,
              descriptor,
              loadThis = false,
              loadArgs = false,
              preCustomizer = {
                // We do not use loadThis() here because we are instrumenting both static and
                // instance methods
                visitVarInsn(ALOAD, 0)
                visitFieldInsn(GETSTATIC, type, memoryType.name, "L$type;")
              },
          )
      return MethodExitVisitor(
          eMv,
          Runtime::onAtomicOperationDone,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = false,
          addFinalBlock = true,
          thisType = className)
    }
    return mv
  }

  fun memoryTypeFromMethodName(name: String): org.pastalab.fray.runtime.MemoryOpType {
    val lname = name.lowercase()
    return if (lname.contains("set") || lname.contains("exchange")) {
      org.pastalab.fray.runtime.MemoryOpType.MEMORY_WRITE
    } else {
      org.pastalab.fray.runtime.MemoryOpType.MEMORY_READ
    }
  }

  companion object {
    val atomicClasses: List<String> =
        listOf(
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
            LongAdder::class.java.name.replace('.', '/'),
        )
    val atomicNonVolatileMethodNames: List<String> =
        mutableListOf(
            "<init>",
            "<clinit>",
            "getPlain",
            "setPlain",
            "toString",
            "weakCompareAndSetPlain",
            "length",
            "hashCode",
            "equals",
            "clone",
            "getClass",
        )
  }
}
