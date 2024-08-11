package org.pastalab.fray.instrumentation.base.visitors

import java.io.PrintStream
import java.lang.invoke.CallSite
import java.lang.invoke.MethodType
import java.util.ServiceLoader
import java.util.logging.Logger
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class SkipMethodInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(
        cv,
        ClassLoader::class.java.name,
        Logger::class.java.name,
        MethodType::class.java.name,
        ServiceLoader::class.java.name,
        PrintStream::class.java.name,
        "java.util.ServiceLoader\$LazyClassPathLookupIterator",
        "sun.reflect.annotation.AnnotationParser",
        CallSite::class.java.name,
    ) {

  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    val methodSignature = "$className#$name$descriptor"
    val eMv =
        MethodEnterVisitor(
            mv,
            org.pastalab.fray.runtime.Runtime::onSkipMethod,
            access,
            name,
            descriptor,
            false,
            false,
            preCustomizer = { push(methodSignature) })
    return MethodExitVisitor(
        eMv,
        org.pastalab.fray.runtime.Runtime::onSkipMethodDone,
        access,
        name,
        descriptor,
        false,
        false,
        true,
        { push(methodSignature) })
  }
}
