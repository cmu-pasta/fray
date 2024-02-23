package cmu.pasta.sfuzz.jdk.jlink

import cmu.pasta.sfuzz.jdk.visitors.ThreadClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.InputStream

fun instrumentClass(path:String, inputStream: InputStream): ByteArray {
    var classReader = ClassReader(inputStream)
    var classWriter = ClassWriter(classReader, 0)
    var classVisitor = ThreadClassVisitor(classWriter)
    classReader.accept(classVisitor, 0)
    return classWriter.toByteArray()
}