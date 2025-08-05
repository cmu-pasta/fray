package org.pastalab.fray.instrumentation.base

import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.util.CheckClassAdapter
import org.pastalab.fray.instrumentation.base.visitors.ArrayOperationInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.ClassConstructorInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.ClassVersionInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.ConditionInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.LoadClassInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.MonitorInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.ObjectNotifyInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.SkipPrimitiveInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.SkipScheduleInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.SleepInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.SynchronizedMethodInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.TargetExitInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.TimeInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.TimedWaitInstrumenter
import org.pastalab.fray.instrumentation.base.visitors.VolatileFieldsInstrumenter
import org.pastalab.fray.runtime.Runtime

class ApplicationCodeTransformer(val interleaveAllMemoryOps: Boolean = false) :
    ClassFileTransformer {
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
        dotClassName.startsWith("org.jetbrains.") ||
        dotClassName.startsWith("worker.org.gradle.") ||
        dotClassName.startsWith(
            "com.github.ajalt",
        ) ||
        (dotClassName.startsWith("org.pastalab.fray") &&
            !dotClassName.startsWith("org.pastalab.fray.example") &&
            !dotClassName.startsWith("org.pastalab.fray.benchmark") &&
            !dotClassName.startsWith("org.pastalab.fray.test") &&
            !dotClassName.startsWith("org.pastalab.fray.junit.internal") &&
            !dotClassName.startsWith(
                "org.pastalab.fray.core.test",
            ))) {
      // This is likely a JDK class, so skip transformation
      return classfileBuffer
    }
    try {
      Runtime.onSkipPrimitive("instrumentation")
      if (Configs.DEBUG_MODE) {
        Utils.writeClassFile(className, classfileBuffer, false)
      }
      val classReader = ClassReader(classfileBuffer)
      val cn = ClassNode()
      var cv: ClassVisitor = ObjectNotifyInstrumenter(cn)
      cv = TargetExitInstrumenter(cv)
      cv = TimedWaitInstrumenter(cv)
      cv = VolatileFieldsInstrumenter(cv, false, interleaveAllMemoryOps)
      cv = ObjectNotifyInstrumenter(cv)

      cv =
          SynchronizedMethodInstrumenter(
              cv, false) // Synchronized Method Instrumenter should be before Monitor Instrumenter
      cv = MonitorInstrumenter(cv)

      cv = ConditionInstrumenter(cv)
      cv = ClassConstructorInstrumenter(cv, false)
      cv = SleepInstrumenter(cv, false)
      cv = TimeInstrumenter(cv)
      cv = SkipPrimitiveInstrumenter(cv)
      cv = SkipScheduleInstrumenter(cv)
      cv = LoadClassInstrumenter(cv)
      //      cv = ObjectHashCodeInstrumenter(cv, false)
      //      cv = AtomicGetInstrumenter(cv)
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
      if (Configs.DEBUG_MODE) {
        Utils.writeClassFile(className, out, true)
      }
      return out
    } catch (e: Throwable) {
      println("Failed to instrument: $className")
      e.printStackTrace()
    } finally {
      Runtime.onSkipPrimitiveDone("instrumentation")
    }
    return classfileBuffer
  }
}
