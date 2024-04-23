package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class UnsafeInstrumenter(cv: ClassVisitor) : ClassVisitorBase(cv, "sun.misc.Unsafe") {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    if (name == "compareAndSwapObject" ||
        name == "compareAndSwapInt" ||
        name == "compareAndSwapLong" ||
        name == "compareAndSetReference") {
      return MethodEnterVisitor(
          mv, Runtime::onUnsafeWriteVolatile, access, name, descriptor, false, true) {
            if (name == "compareAndSwapLong") {
              pop2()
              pop2()
            } else {
              pop()
              pop()
            }
          }
    }
    if (name == "getObjectVolatile" ||
        name == "getIntVolatile" ||
        name == "getLongVolatile" ||
        name == "getBooleanVolatile" ||
        name == "getByteVolatile" ||
        name == "getShortVolatile" ||
        name == "getCharVolatile" ||
        name == "getFloatVolatile" ||
        name == "getDoubleVolatile") {
      return MethodEnterVisitor(
          mv, Runtime::onUnsafeReadVolatile, access, name, descriptor, false, true)
    }
    if (name == "putObjectVolatile" ||
        name == "putIntVolatile" ||
        name == "putLongVolatile" ||
        name == "putBooleanVolatile" ||
        name == "putByteVolatile" ||
        name == "putShortVolatile" ||
        name == "putCharVolatile" ||
        name == "putFloatVolatile" ||
        name == "putDoubleVolatile" ||
        name == "getAndAddLong" ||
        name == "getAndAddInt" ||
        name == "getAndSetLong" ||
        name == "getAndSetInt" ||
        name == "getAndSetObject") {
      return MethodEnterVisitor(
          mv, Runtime::onUnsafeWriteVolatile, access, name, descriptor, false, true) {
            if (name == "putLongVolatile" || name == "putDoubleVolatile") {
              pop2()
            } else {
              pop()
            }
          }
    }
    return mv
  }
}
