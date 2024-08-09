package org.pastalab.fray.instrumentation.agent

import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.pastalab.fray.instrumentation.visitors.*

class ApplicationCodeTransformer : ClassFileTransformer {
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
    if (dotClassName.startsWith("java.") ||
        dotClassName.startsWith("javax.") ||
        dotClassName.startsWith("jdk.") ||
        dotClassName.startsWith("sun.") ||
        dotClassName.startsWith("com.sun.") ||
        dotClassName.startsWith("kotlin.") ||
        dotClassName.startsWith("kotlinx.") ||
        (dotClassName.startsWith("org.junit.") && !dotClassName.contains("ConsoleLauncher")) ||
        dotClassName.startsWith("org.gradle.") ||
        dotClassName.startsWith("worker.org.gradle.") ||
        dotClassName.startsWith("com.github.ajalt") ||
        (dotClassName.startsWith("org.pastalab.fray") &&
            !dotClassName.startsWith("org.pastalab.fray.benchmark") &&
            !dotClassName.startsWith("org.pastalab.fray.it"))) {
      // This is likely a JDK class, so skip transformation
      return classfileBuffer
    }
    File("/tmp/out/origin/${className.replace("/", ".").removePrefix(".")}.class")
        .writeBytes(classfileBuffer)
    try {
      val classReader = ClassReader(classfileBuffer)
      val cn = ClassNode()
      var cv: ClassVisitor = ObjectNotifyInstrumenter(cn)
      cv = TargetExitInstrumenter(cv)
      cv = TimedWaitInstrumenter(cv)
      cv = VolatileFieldsInstrumenter(cv, false)
      cv = ObjectNotifyInstrumenter(cv)
      cv = MonitorInstrumenter(cv)
      cv = ConditionInstrumenter(cv)
      cv = SynchronizedMethodInstrumenter(cv, false)
      cv = ClassConstructorInstrumenter(cv)
      cv = SleepInstrumenter(cv)
      cv = TimeInstrumenter(cv)
      cv = ThreadHashCodeInstrumenter(cv)
      cv = AtomicGetInstrumenter(cv)
      cv = ToStringInstrumenter(cv)
      val classVersionInstrumenter = ClassVersionInstrumenter(cv)
      cv = ArrayOperationInstrumenter(classVersionInstrumenter)
      classReader.accept(cv, ClassReader.EXPAND_FRAMES)
      val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
      if (classVersionInstrumenter.classVersion >= Opcodes.V1_5) {
        cn.accept(classWriter)
        //        val checkClassAdapter = CheckClassAdapter(classWriter)
        //        cn.accept(checkClassAdapter)
      } else {
        cn.accept(classWriter)
      }
      val out = classWriter.toByteArray()
      File("/tmp/out/app/${className.replace("/",
          ".").removePrefix(".")}.class")
          .writeBytes(out)
      return out
    } catch (e: Throwable) {
      println("Failed to instrument: $className")
      e.printStackTrace()
    }
    return classfileBuffer
  }
}
