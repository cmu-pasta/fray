package org.pastalab.fray.instrumentation.base

import java.io.File
import java.io.InputStream
import java.util.concurrent.locks.ReentrantReadWriteLock
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
  val shouldSkipChecking =
      path.contains("SystemModules") || path.contains("$") || path.contains("module-info")

  try {
    val classReader = ClassReader(byteArray)
    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
    val cn = ClassNode()
    var cv: ClassVisitor = ThreadInstrumenter(cn)
    cv = VolatileFieldsInstrumenter(cv, instrumentingJdk = true, interleaveAllMemoryOps = false)
    cv = SleepInstrumenter(cv, true)
    cv = VarHandleInstrumenter(cv)
    cv =
        ReentrantReadWriteLockInstrumenter(
            cv,
            ReentrantReadWriteLock.ReadLock::class.java.name.replace('.', '/'),
        )
    cv =
        ReentrantReadWriteLockInstrumenter(
            cv,
            ReentrantReadWriteLock.WriteLock::class.java.name.replace('.', '/'),
        )
    cv = LockSupportInstrumenter(cv)
    cv = LockInstrumenter(cv)
    cv = SystemModulesMapInstrumenter(cv)
    cv = ConditionInstrumenter(cv)
    cv = AtomicOperationInstrumenter(cv)
    cv = ObjectNotifyInstrumenter(cv)
    cv = UnsafeInstrumenter(cv)
    cv = SkipPrimitiveInstrumenter(cv)
    cv = SkipScheduleInstrumenter(cv)
    cv = ObjectInstrumenter(cv)
    cv = ForkJoinPoolCommonInstrumenter(cv)
    cv = ForkJoinPoolManagedBlockInstrumenter(cv)
    cv = UnsafeParkInstrumenter(cv)
    cv = NioSocketImplInstrumenter(cv)
    cv = ClassConstructorInstrumenter(cv, true)
    //    cv = ObjectHashCodeInstrumenter(cv, true)
    cv = SelectorVisitor(cv)
    cv = SocketChannelInstrumenter(cv)
    cv = ServerSocketChannelInstrumenter(cv)
    cv = AbstractInterruptibleChannelVisitor(cv)
    cv = PipedInputStreamInstrumenter(cv)
    cv = SemaphoreInstrumenter(cv)
    cv = StampedLockInstrumenter(cv)
    cv = CountDownLatchInstrumenter(cv)
    cv = MethodHandleNativesInstrumenter(cv)
    cv = TimedWaitInstrumenter(cv)
    cv = ThreadLocalRandomInstrumenter(cv)
    cv =
        SynchronizedMethodInstrumenter(
            cv,
            true,
        ) // Synchronized Method Instrumenter should be before Monitor Instrumenter
    cv = MonitorInstrumenter(cv)
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
    e.printStackTrace()
    throw e
  }
}

fun instrumentModuleInfo(inputStream: InputStream, packages: List<String>): ByteArray {
  val cn = ClassNode()
  val cr = ClassReader(inputStream)

  val attrs =
      mutableListOf(ModuleTargetAttribute(), ModuleResolutionAttribute(), ModuleHashesAttribute())
  cr.accept(cn, attrs.toTypedArray(), 0)
  cn.module.exports.add(ModuleExportNode("org/pastalab/fray/runtime", 0, null))
  cn.module.packages.addAll(packages)
  val cw = ClassWriter(0)
  cn.accept(cw)
  val out = cw.toByteArray()
  if (DEBUG_MODE) {
    writeClassFile("java.base.module-info.class", out, true)
  }
  return out
}

/**
 * To run this main method you need to add --patch-module org.pastalab.fray.instrumentation.base=
 * PATH_TO_FRAY/instrumentation/base/build/classes/kotlin/main
 */
fun main(args: Array<String>) {
  val inputStream = File(args[0]).inputStream()
  instrumentClass(args[0], inputStream)
}
