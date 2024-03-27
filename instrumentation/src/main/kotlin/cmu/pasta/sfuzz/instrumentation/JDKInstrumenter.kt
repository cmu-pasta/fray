package cmu.pasta.sfuzz.instrumentation

import cmu.pasta.sfuzz.instrumentation.visitors.*
import java.io.File
import java.io.InputStream
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ModuleHashesAttribute
import org.objectweb.asm.commons.ModuleResolutionAttribute
import org.objectweb.asm.commons.ModuleTargetAttribute
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.ModuleExportNode

fun instrumentClass(path: String, inputStream: InputStream): ByteArray {
  val byteArray = inputStream.readBytes()
  try {
    val classReader = ClassReader(byteArray)
    //    var classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES or
    // ClassWriter.COMPUTE_MAXS)
    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
    var cv: ClassVisitor = ThreadInstrumenter(classWriter)
    cv = ReentrantReadWriteLockInstrumenter(cv)
    cv = LockSupportInstrumenter(cv)
    cv = LockInstrumenter(cv)
    cv = SystemModulesMapInstrumenter(cv)
    cv = ConditionInstrumenter(cv)
    cv = AtomicOperationInstrumenter(cv)
    cv = ObjectNotifyInstrumenter(cv)
    cv = VolatileFieldsInstrumenter(cv, true)
    cv = UnsafeInstrumenter(cv)
    cv = ClassloaderInstrumenter(cv)
    cv = ObjectInstrumenter(cv)
    cv = SemaphoreInstrumenter(cv)
    cv = CountDownLatchInstrumenter(cv)
    // MonitorInstrumenter should come second because ObjectInstrumenter will insert more
    // monitors.
    cv = MonitorInstrumenter(cv)
    // SynchronizedMethodEmbeddingInstrumenter should come before MonitorInstrumenter because
    // it inlines monitors for synchronized methods.
    //    cv = SynchronizedMethodInstrumenter(cv)
    classReader.accept(cv, ClassReader.EXPAND_FRAMES)
    val out = classWriter.toByteArray()
    File("/tmp/out/jdk/${path.replace("/", ".").removePrefix(".")}").writeBytes(out)
    //        File("/tmp/${path.replace("/", ".").removePrefix(".")}").writeBytes(out)
    return out
  } catch (e: Throwable) {
    println("Exception during instrumentation: $e")
    e.printStackTrace()
  }
  File("/tmp/out/jdk/${path.replace("/", ".").removePrefix(".")}").writeBytes(byteArray)
  return byteArray
}

fun instrumentModuleInfo(inputStream: InputStream, packages: List<String>): ByteArray {
  var cn = ClassNode()
  var cr = ClassReader(inputStream)

  var attrs =
      mutableListOf(ModuleTargetAttribute(), ModuleResolutionAttribute(), ModuleHashesAttribute())
  cr.accept(cn, attrs.toTypedArray(), 0)
  cn.module.exports.add(ModuleExportNode("cmu/pasta/sfuzz/runtime", 0, null))
  cn.module.packages.addAll(packages)
  var cw = ClassWriter(0)
  cn.accept(cw)
  var out = cw.toByteArray()
  File("/tmp/out/java.base.module-info.class").writeBytes(out)
  return out
}
