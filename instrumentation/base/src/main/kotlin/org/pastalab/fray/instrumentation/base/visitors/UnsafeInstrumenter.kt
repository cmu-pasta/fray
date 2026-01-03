package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class UnsafeInstrumenter(cv: ClassVisitor) : ClassVisitorBase(cv, "sun.misc.Unsafe") {
  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?,
  ): MethodVisitor {
    if (
        name == "compareAndSwapObject" ||
            name == "compareAndSwapInt" ||
            name == "compareAndSwapLong" ||
            name == "compareAndSetReference"
    ) {
      return MethodEnterVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onUnsafeWriteVolatile,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = true,
          preCustomizer = {
            if (name == "compareAndSwapLong") {
              pop2()
              pop2()
            } else {
              pop()
              pop()
            }
          },
      )
    }
    if (
        name == "getObjectVolatile" ||
            name == "getIntVolatile" ||
            name == "getLongVolatile" ||
            name == "getBooleanVolatile" ||
            name == "getByteVolatile" ||
            name == "getShortVolatile" ||
            name == "getCharVolatile" ||
            name == "getFloatVolatile" ||
            name == "getDoubleVolatile"
    ) {
      return MethodEnterVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onUnsafeReadVolatile,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = true,
      )
    }
    if (
        name == "putObjectVolatile" ||
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
            name == "getAndSetObject"
    ) {
      return MethodEnterVisitor(
          mv,
          org.pastalab.fray.runtime.Runtime::onUnsafeWriteVolatile,
          access,
          name,
          descriptor,
          loadThis = false,
          loadArgs = true,
          preCustomizer = {
            if (
                name == "putLongVolatile" ||
                    name == "putDoubleVolatile" ||
                    name == "getAndAddLong" ||
                    name == "getAndSetLong"
            ) {
              pop2()
            } else {
              pop()
            }
          },
      )
    }
    return mv
  }
}
