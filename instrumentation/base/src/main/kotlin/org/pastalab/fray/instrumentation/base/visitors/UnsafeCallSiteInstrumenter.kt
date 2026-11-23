package org.pastalab.fray.instrumentation.base.visitors

import kotlin.reflect.KFunction
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.pastalab.fray.runtime.Runtime

/**
 * Adds a scheduling point before direct `jdk.internal.misc.Unsafe` volatile accesses and atomic
 * updates in `java.util.concurrent` classes (e.g. `ConcurrentHashMap`), so that another thread may
 * run between a read and the compare-and-set that follows it.
 */
class UnsafeCallSiteInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  private var shouldInstrument = false

  override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?,
  ) {
    super.visit(version, access, name, signature, superName, interfaces)
    shouldInstrument = name.startsWith("java/util/concurrent/") && name != LOCK_SUPPORT
  }

  override fun visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?,
  ): MethodVisitor {
    val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
    if (!shouldInstrument || name == "<init>" || name == "<clinit>") {
      return mv
    }
    return object : AdviceAdapter(ASM9, mv, access, name, descriptor) {
      // The locals created below only live between the hook and the Unsafe call, so they must
      // not show up in the stack map frames of the rest of the method.
      override fun updateNewLocals(newLocals: Array<Any?>) {
        for (i in newLocals.indices) {
          if (newLocals[i] != null) {
            newLocals[i] = Opcodes.TOP
          }
        }
      }

      override fun visitMethodInsn(
          opcodeAndSource: Int,
          owner: String,
          name: String,
          descriptor: String,
          isInterface: Boolean,
      ) {
        val hook =
            if (owner == UNSAFE && descriptor.startsWith(OBJECT_OFFSET)) hookFor(name) else null
        if (hook != null) {
          // Stack: unsafe, object, offset, args... We need (object, offset) for the hook.
          val args = Type.getArgumentTypes(descriptor).drop(2).map { newLocal(it) }
          args.reversed().forEach { storeLocal(it) }
          val offset = newLocal(Type.LONG_TYPE)
          storeLocal(offset)
          dup()
          loadLocal(offset)
          invokeStatic(RUNTIME, Utils.kFunctionToASMMethod(hook))
          loadLocal(offset)
          args.forEach { loadLocal(it) }
        }
        super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
      }
    }
  }

  companion object {
    private const val UNSAFE = "jdk/internal/misc/Unsafe"
    // LockSupport only uses these accessors for the park blocker of a thread, which is
    // diagnostic state, and parking itself is modeled through LockSupportInstrumenter.
    private const val LOCK_SUPPORT = "java/util/concurrent/locks/LockSupport"
    private const val OBJECT_OFFSET = "(Ljava/lang/Object;J"
    private val RUNTIME = Type.getObjectType(Runtime::class.java.name.replace(".", "/"))
    private val ATOMIC_UPDATE_PREFIXES =
        listOf(
            "compareAndSet",
            "compareAndExchange",
            "weakCompareAndSet",
            "getAndSet",
            "getAndAdd",
            "getAndBitwise",
        )
    private val VOLATILE_READ_SUFFIXES = listOf("Volatile", "Acquire", "Opaque")
    private val VOLATILE_WRITE_SUFFIXES = listOf("Volatile", "Release", "Opaque")

    fun hookFor(name: String): KFunction<*>? {
      if (ATOMIC_UPDATE_PREFIXES.any { name.startsWith(it) }) {
        return Runtime::onUnsafeWriteVolatile
      }
      if (name.startsWith("put") && VOLATILE_WRITE_SUFFIXES.any { name.endsWith(it) }) {
        return Runtime::onUnsafeWriteVolatile
      }
      if (name.startsWith("get") && VOLATILE_READ_SUFFIXES.any { name.endsWith(it) }) {
        return Runtime::onUnsafeReadVolatile
      }
      return null
    }
  }
}
