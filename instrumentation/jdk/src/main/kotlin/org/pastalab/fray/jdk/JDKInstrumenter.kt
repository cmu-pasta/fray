package org.pastalab.fray.jdk

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
import org.pastalab.fray.instrumentation.base.Configs.DEBUG_MODE
import org.pastalab.fray.instrumentation.base.Utils.writeClassFile
import org.pastalab.fray.instrumentation.base.visitors.*

fun instrumentClass(path: String, inputStream: InputStream): ByteArray {
  val byteArray = inputStream.readBytes()
  if (DEBUG_MODE) {
    writeClassFile(path, byteArray, false)
  }
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
    //    cv = PrintStreamInstrumenter(cv)
    cv = ObjectInstrumenter(cv)
    cv = SemaphoreInstrumenter(cv)
    cv = CountDownLatchInstrumenter(cv)
    cv = MethodHandleNativesInstrumenter(cv)
    cv = TimedWaitInstrumenter(cv)
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
    if (DEBUG_MODE) {
      writeClassFile(path, out, true)
    }
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
  cn.module.exports.add(ModuleExportNode("org/pastalab/fray/runtime", 0, null))
  cn.module.packages.addAll(packages)
  var cw = ClassWriter(0)
  cn.accept(cw)
  var out = cw.toByteArray()
  if (DEBUG_MODE) {
    writeClassFile("java.base.module-info.class", out, true)
  }
  return out
}
