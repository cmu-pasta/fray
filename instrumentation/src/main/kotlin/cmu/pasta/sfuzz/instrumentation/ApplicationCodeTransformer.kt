package cmu.pasta.sfuzz.instrumentation

import cmu.pasta.sfuzz.instrumentation.visitors.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

class ApplicationCodeTransformer: ClassFileTransformer {
    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray {
        val dotClassName = className.replace('/', '.')
        // Check if the class loader is null (bootstrap class loader)
        // and if the class name starts with known JDK prefixes.
        if (dotClassName.startsWith("java.")
            || dotClassName.startsWith("javax.")
            || dotClassName.startsWith("jdk.")
            || dotClassName.startsWith("sun.")
            || dotClassName.startsWith("com.sun.")
            || dotClassName.startsWith("kotlin.")
            || dotClassName.startsWith("kotlinx.")
            || dotClassName.startsWith("cmu.pasta.sfuzz")) {
            // This is likely a JDK class, so skip transformation
            return classfileBuffer
        }
        var classReader = ClassReader(classfileBuffer)
        var classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)


        try {
            var cv: ClassVisitor = ObjectNotifyInstrumenter(classWriter)
            cv = TargetExitInstrumenter(cv)
            cv = VolatileFieldsInstrumenter(cv)
            cv = ObjectNotifyInstrumenter(cv)
            cv = MonitorInstrumenter(cv)
            cv = SynchronizedMethodInstrumenter(cv)
            cv = ClassVersionInstrumenter(cv)
            classReader.accept(cv, ClassReader.EXPAND_FRAMES)
            val out = classWriter.toByteArray()
            File("/tmp/out/${className.replace("/", ".").removePrefix(".")}.class").writeBytes(out)
            return out
        } catch (e: Throwable) {
            println("Failed to instrument: $className")
            e.printStackTrace()
        }
        return classfileBuffer
    }
}