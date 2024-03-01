package cmu.pasta.sfuzz.instrumentation

import cmu.pasta.sfuzz.instrumentation.visitors.MonitorInstrumenter
import cmu.pasta.sfuzz.instrumentation.visitors.ObjectInstrumenter
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
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
                    || dotClassName.startsWith("kotlin.")
                    || dotClassName.startsWith("kotlinx.")
                    || dotClassName.startsWith("cmu.pasta.sfuzz")) {
            // This is likely a JDK class, so skip transformation
            return classfileBuffer
        }
        var classReader = ClassReader(classfileBuffer)
        var classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)

        var cv: ClassVisitor = ObjectInstrumenter(classWriter)
        cv = MonitorInstrumenter(cv)
        classReader.accept(cv, 0)
        return classWriter.toByteArray()
    }
}