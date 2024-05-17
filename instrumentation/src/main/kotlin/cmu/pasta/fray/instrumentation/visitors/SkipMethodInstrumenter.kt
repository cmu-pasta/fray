package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
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
        "java.util.ServiceLoader\$LazyClassPathLookupIterator") {

  override fun instrumentMethod(
      mv: MethodVisitor,
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
  ): MethodVisitor {
    //    if ((name == "loadClass" &&
    //        descriptor == "(Ljava/lang/String;)Ljava/lang/Class;") ||
    //        name == "makeImpl" ||
    //        className == "java/util/logging/Logger") {
    val eMv = MethodEnterVisitor(mv, Runtime::onSkipMethod, access, name, descriptor, false, false)
    return MethodExitVisitor(
        eMv, Runtime::onSkipMethodDone, access, name, descriptor, false, false, true)
    //    }
    return mv
  }
}
