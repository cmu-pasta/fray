package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class UnsafeInstrumenter(cv: ClassVisitor): ClassVisitorBase(cv, "sun.misc.Unsafe") {
    override fun instrumentMethod(
        mv: MethodVisitor,
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        // TODO(aoli): consider passing more information (memory location, ect)
        if (unsafeMethodNames.contains(name)) {
            return MethodEnterVisitor(mv, Runtime::onUnsafeOperation, access, name, descriptor, false, false)
        }
        return mv
    }

    companion object {
        private val unsafeMethodNames: List<String> = mutableListOf(
            "compareAndSwapInt",
            "compareAndSwapObject",
            "compareAndSwapLong",
            "getObjectVolatile",
            "putObjectVolatile",
            "getIntVolatile",
            "putIntVolatile",
            "getBooleanVolatile",
            "putBooleanVolatile",
            "getByteVolatile",
            "putByteVolatile",
            "getShortVolatile",
            "putShortVolatile",
            "getCharVolatile",
            "putCharVolatile",
            "getLongVolatile",
            "putLongVolatile",
            "getFloatVolatile",
            "putFloatVolatile",
            "getDoubleVolatile",
            "putDoubleVolatile",
            "getAndAddInt",  // (atomic but not volatile semantics)
            "getAndAddLong",  // (atomic but not volatile semantics)
            "getAndSetInt",  // (atomic but not volatile semantics)
            "getAndSetLong",  // (atomic but not volatile semantics)
            "getAndSetObject" // (atomic but not volatile semantics)
        )
    }
}