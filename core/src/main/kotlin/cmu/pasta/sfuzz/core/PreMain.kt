package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.instrumentation.ApplicationCodeTransformer
import cmu.pasta.sfuzz.runtime.Runtime
import java.lang.instrument.Instrumentation

fun premain(arguments: String?, instrumentation: Instrumentation) {
    println("Premain launched on thread ${Thread.currentThread().threadId()}")
    Runtime.DELEGATE = RuntimeDelegate()
    GlobalContext.registerThread(Thread.currentThread())
    GlobalContext.currentThreadId = Thread.currentThread().threadId()
    instrumentation.addTransformer(ApplicationCodeTransformer())
    println("Premain done!")
}