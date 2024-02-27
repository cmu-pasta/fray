package cmu.pasta.sfuzz.instrumentation.visitors

import cmu.pasta.sfuzz.runtime.Runtime
import org.objectweb.asm.Opcodes.ASM9
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import java.lang.invoke.VarHandle
import java.util.*
import java.util.concurrent.atomic.*

class AtomicOperationClassVisitor(cv: ClassVisitor): ClassVisitor(ASM9, cv) {
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
        var mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (atomicClasses.contains(className) && !atomicNonVolatileMethodNames.contains(name)
            && access and ACC_PUBLIC != 0) {
            return MethodEnterInstrumenter(mv, Runtime::onAtomicOperation)
        }
        return mv
    }

    companion object {
        val atomicClasses: List<String> = Arrays.asList(
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
            LongAdder::class.java.name.replace('.', '/')
        )
        val atomicNonVolatileMethodNames: List<String> = mutableListOf(
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
            "getClass"
        )
    }
}