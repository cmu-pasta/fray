package org.pastalab.fray.instrumentation.base.visitors

import java.io.PrintStream
import java.lang.invoke.CallSite
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.net.JarURLConnection
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.security.Provider
import java.util.Properties
import java.util.ResourceBundle
import java.util.ServiceLoader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor

class SkipMethodInstrumenter(cv: ClassVisitor) :
    ClassVisitorBase(
        cv,
        ClassLoader::class.java.name,
        MethodType::class.java.name,
        ServiceLoader::class.java.name,
        PrintStream::class.java.name,
        URLClassLoader::class.java.name,
        JarURLConnection::class.java.name,
        Properties::class.java.name,
        ResourceBundle::class.java.name,
        Charset::class.java.name,
        Provider::class.java.name,
        "sun.security.util.ObjectIdentifier",
        "org.junit.platform.launcher.core.LauncherConfigurationParameters",
        //        "org.junit.platform.engine.support.store.NamespacedHierarchicalStore",
        "org.slf4j.LoggerFactory",
        "com.intellij.rt.debugger.agent",
        "java.util.ServiceLoader\$LazyClassPathLookupIterator",
        "sun.reflect.annotation.AnnotationParser",
        "java.lang.ProcessImpl",
        "jdk.internal.misc.TerminatingThreadLocal",
        MethodHandle::class.java.name,
        MethodHandles::class.java.name,
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
    if (name == "<init>" || name == "<clinit>") {
      return mv
    }
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
        className,
        customizer = { mv, isFinalBlock -> push(methodSignature) })
  }
}
