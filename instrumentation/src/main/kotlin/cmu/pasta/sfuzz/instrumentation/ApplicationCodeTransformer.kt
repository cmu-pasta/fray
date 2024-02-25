package cmu.pasta.sfuzz.instrumentation

import cmu.pasta.sfuzz.instrumentation.visitors.ObjectNotifyInstrumenter
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
        if (loader == null && (dotClassName.startsWith("java.") || dotClassName.startsWith("javax.")
                    || dotClassName.startsWith("jdk.") || dotClassName.startsWith("sun.")
                    || dotClassName.startsWith("cmu.pasta.sfuzz"))) {
            // This is likely a JDK class, so skip transformation
            return super.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)
        }

        var classReader = ClassReader(classfileBuffer)
        var classWriter = ClassWriter(classReader, 0)

        var cv: ClassVisitor = ObjectNotifyInstrumenter(classWriter)
        classReader.accept(cv, 0)
        return classWriter.toByteArray()
    }
}