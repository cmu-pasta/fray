package cmu.pasta.sfuzz.instrumentation

import cmu.pasta.sfuzz.instrumentation.visitors.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ModuleHashesAttribute
import org.objectweb.asm.commons.ModuleResolutionAttribute
import org.objectweb.asm.commons.ModuleTargetAttribute
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.ModuleExportNode
import java.io.File
import java.io.InputStream


fun instrumentClass(path:String, inputStream: InputStream): ByteArray {
    var classReader = ClassReader(inputStream)
//    var classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
    var classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
    var cv:ClassVisitor = ThreadInstrumenter(classWriter)
    cv = ReentrantReadWriteLockInstrumenter(cv)
    cv = LockSupportInstrumenter(cv)
    cv = LockInstrumenter(cv)
    cv = SystemModulesMapInstrumenter(cv)
    cv = ConditionInstrumenter(cv)
    cv = AtomicOperationInstrumenter(cv)
    cv = ObjectNotifyInstrumenter(cv)
    cv = VolatileFieldsInstrumenter(cv)
    cv = UnsafeInstrumenter(cv)
    cv = ClassloaderInstrumenter(cv)
    cv = ObjectInstrumenter(cv)
    // MonitorInstrumenter should come second because ObjectInstrumenter will insert more
    // monitors.
    cv = MonitorInstrumenter(cv)
    // SynchronizedMethodEmbeddingInstrumenter should come before MonitorInstrumenter because
    // it inlines monitors for synchronized methods.
//    cv = SynchronizedMethodInstrumenter(cv)
    classReader.accept(cv, ClassReader.EXPAND_FRAMES)
    var out = classWriter.toByteArray()
    File("/tmp/out/jdk/${path.replace("/", ".").removePrefix(".")}").writeBytes(out)
    return out
}

fun instrumentModuleInfo(inputStream: InputStream, packages: List<String>):ByteArray {
    var cn = ClassNode()
    var cr = ClassReader(inputStream)

    var attrs = mutableListOf(ModuleTargetAttribute(), ModuleResolutionAttribute(), ModuleHashesAttribute())
    cr.accept(cn, attrs.toTypedArray(), 0)
    cn.module.exports.add(ModuleExportNode("cmu/pasta/sfuzz/runtime", 0, null))
    cn.module.packages.addAll(packages)
    var cw = ClassWriter(0)
    cn.accept(cw)
    var out = cw.toByteArray()
    File("/tmp/out/java.base.module-info.class").writeBytes(out)
    return out
}
