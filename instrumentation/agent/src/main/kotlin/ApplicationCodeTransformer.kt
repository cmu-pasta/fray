package org.pastalab.fray.instrumentation.agent

import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import org.pastalab.fray.instrumentation.base.Configs.DEBUG_MODE
import org.pastalab.fray.instrumentation.base.Utils.writeClassFile
import org.pastalab.fray.instrumentation.base.visitors.*
import org.pastalab.fray.runtime.Runtime

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
        dotClassName.startsWith(
            "jdk.",
        ) ||
        dotClassName.startsWith("sun.") ||
        dotClassName.startsWith("com.sun.") ||
        dotClassName.startsWith(
            "kotlin.",
        ) ||
        dotClassName.startsWith("kotlinx.") ||
        (dotClassName.startsWith("org.junit.") &&
            !(dotClassName.contains("ConsoleLauncher") ||
                //                dotClassName.contains("NamespacedHierarchicalStore") ||
                dotClassName.contains("LauncherConfigurationParameters"))) ||
        dotClassName.startsWith("org.gradle.") ||
        dotClassName.startsWith("worker.org.gradle.") ||
        dotClassName.startsWith(
            "com.github.ajalt",
        ) ||
        (dotClassName.startsWith("org.pastalab.fray") &&
            !dotClassName.startsWith("org.pastalab.fray.benchmark") &&
            !dotClassName.startsWith("org.pastalab.fray.test") &&
            !dotClassName.startsWith(
                "org.pastalab.fray.core.test",
            ))) {
      // This is likely a JDK class, so skip transformation
      return classfileBuffer
    }
    if (DEBUG_MODE) {
      writeClassFile(className, classfileBuffer, false)
    }
    try {
      Runtime.onSkipMethod("instrumentation")
      val classReader = ClassReader(classfileBuffer)
      val cn = ClassNode()
      var cv: ClassVisitor = ObjectNotifyInstrumenter(cn)
      cv = TargetExitInstrumenter(cv)
      cv = TimedWaitInstrumenter(cv)
      cv = VolatileFieldsInstrumenter(cv, false)
      cv = ObjectNotifyInstrumenter(cv)

      cv =
          SynchronizedMethodInstrumenter(
              cv, false) // Synchronized Method Instrumenter should be before Monitor Instrumenter
      cv = MonitorInstrumenter(cv)

      cv = ConditionInstrumenter(cv)
      cv = ClassConstructorInstrumenter(cv)
      cv = SleepInstrumenter(cv)
      cv = TimeInstrumenter(cv)
      cv = SkipMethodInstrumenter(cv)
      cv = ObjectHashCodeInstrumenter(cv, false)
      cv = AtomicGetInstrumenter(cv)
      //      cv = ToStringInstrumenter(cv)
      val classVersionInstrumenter = ClassVersionInstrumenter(cv)
      cv = ArrayOperationInstrumenter(classVersionInstrumenter)
      classReader.accept(cv, ClassReader.EXPAND_FRAMES)
      val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
      if (classVersionInstrumenter.classVersion >= Opcodes.V1_5) {
        val checkClassAdapter = CheckClassAdapter(classWriter)
        cn.accept(checkClassAdapter)
      } else {
        cn.accept(classWriter)
      }
      val out = classWriter.toByteArray()
      if (DEBUG_MODE) {
        writeClassFile(className, out, true)
      }
      return out
    } catch (e: Throwable) {
      println("Failed to instrument: $className")
      e.printStackTrace()
    } finally {
      Runtime.onSkipMethodDone("instrumentation")
    }
    return classfileBuffer
  }
}
