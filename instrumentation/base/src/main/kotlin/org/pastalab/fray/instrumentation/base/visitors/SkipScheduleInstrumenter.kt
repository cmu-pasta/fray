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

class SkipScheduleInstrumenter(cv: ClassVisitor) :
    SkipMethodInstrumenter(
        cv,
        org.pastalab.fray.runtime.Runtime::onSkipScheduling,
        org.pastalab.fray.runtime.Runtime::onSkipSchedulingDone,
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
        "sun.instrument.InstrumentationImpl",
        "org.junit.platform.launcher.core.LauncherConfigurationParameters",
        //        "org.junit.platform.engine.support.store.NamespacedHierarchicalStore",
        "org.slf4j.LoggerFactory",
        "java.util.ServiceLoader\$LazyClassPathLookupIterator",
        "sun.reflect.annotation.AnnotationParser",
        "java.security.Signature",
        "jdk.internal.misc.TerminatingThreadLocal",
        MethodHandle::class.java.name,
        MethodHandles::class.java.name,
        CallSite::class.java.name,
    )
