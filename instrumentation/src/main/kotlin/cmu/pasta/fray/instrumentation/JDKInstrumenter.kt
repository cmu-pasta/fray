package cmu.pasta.fray.instrumentation

import cmu.pasta.fray.instrumentation.visitors.*
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
import org.objectweb.asm.util.CheckClassAdapter

fun instrumentClass(path: String, inputStream: InputStream): ByteArray {
  val byteArray = inputStream.readBytes()
  File("/tmp/out/origin/${path.replace("/", ".").removePrefix(".")}").writeBytes(byteArray)
  val shouldSkipChecking = path.contains("SystemModules") || path.contains("$")

  try {
    val classReader = ClassReader(byteArray)
    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
    val cn = ClassNode()
    var cv: ClassVisitor = ThreadInstrumenter(cn)
    cv = VolatileFieldsInstrumenter(cv, true)
    cv = FieldInstanceReadWriteInstrumenter(cv)
    cv = ReentrantReadWriteLockInstrumenter(cv)
    cv = LockSupportInstrumenter(cv)
    cv = LockInstrumenter(cv)
    cv = SystemModulesMapInstrumenter(cv)
    cv = ConditionInstrumenter(cv)
    cv = AtomicOperationInstrumenter(cv)
    cv = ObjectNotifyInstrumenter(cv)
    cv = UnsafeInstrumenter(cv)
    cv = SkipMethodInstrumenter(cv)
    cv = PrintStreamInstrumenter(cv)
    cv = ObjectInstrumenter(cv)
    cv = SemaphoreInstrumenter(cv)
    cv = CountDownLatchInstrumenter(cv)
    cv = MethodHandleNativesInstrumenter(cv)
    cv = ThreadParkInstrumenter(cv)
    // MonitorInstrumenter should come second because ObjectInstrumenter will insert more
    // monitors.
    cv = MonitorInstrumenter(cv)
    // SynchronizedMethodEmbeddingInstrumenter should come before MonitorInstrumenter because
    // it inlines monitors for synchronized methods.
    cv = SynchronizedMethodInstrumenter(cv, true)
    classReader.accept(cv, ClassReader.EXPAND_FRAMES)
    if (shouldSkipChecking) {
      cn.accept(classWriter)
    } else {
      cn.accept(CheckClassAdapter(classWriter))
    }
    val out = classWriter.toByteArray()
    File("/tmp/out/jdk/${path.replace("/", ".").removePrefix(".")}").writeBytes(out)
    //        File("/tmp/${path.replace("/", ".").removePrefix(".")}").writeBytes(out)
    return out
  } catch (e: Throwable) {
    println(path)
    throw e
  }
}

fun instrumentModuleInfo(inputStream: InputStream, packages: List<String>): ByteArray {
  var cn = ClassNode()
  var cr = ClassReader(inputStream)

  var attrs =
      mutableListOf(ModuleTargetAttribute(), ModuleResolutionAttribute(), ModuleHashesAttribute())
  cr.accept(cn, attrs.toTypedArray(), 0)
  cn.module.exports.add(ModuleExportNode("cmu/pasta/fray/runtime", 0, null))
  cn.module.packages.addAll(packages)
  var cw = ClassWriter(0)
  cn.accept(cw)
  var out = cw.toByteArray()
  File("/tmp/out/java.base.module-info.class").writeBytes(out)
  return out
}
