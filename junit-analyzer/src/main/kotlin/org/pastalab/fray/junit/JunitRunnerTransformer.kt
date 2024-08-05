package org.pastalab.fray.junit

import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

class JunitRunnerTransformer : ClassFileTransformer {
  override fun transform(
      loader: ClassLoader?,
      className: String,
      classBeingRedefined: Class<*>?,
      protectionDomain: ProtectionDomain?,
      classfileBuffer: ByteArray
  ): ByteArray {
    val classReader = ClassReader(classfileBuffer)
    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
    var cv: ClassVisitor = JunitInstrumenter(classWriter)
    cv = OutcomeDelayingEngineExecutionListenerInstrumenter(cv)
    classReader.accept(cv, ClassReader.EXPAND_FRAMES)
    return classWriter.toByteArray()
  }
}
