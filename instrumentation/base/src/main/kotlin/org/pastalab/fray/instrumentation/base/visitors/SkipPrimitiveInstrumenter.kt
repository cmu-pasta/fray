package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor

class SkipPrimitiveInstrumenter(cv: ClassVisitor) :
    SkipMethodInstrumenter(
        cv,
        org.pastalab.fray.runtime.Runtime::onSkipPrimitive,
        org.pastalab.fray.runtime.Runtime::onSkipPrimitiveDone,
        "java.lang.ProcessImpl",
        "com.intellij.rt.debugger",
        "org.gradle.internal",
        "org.mockito.Mockito",
    )
