package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.instrumentation.ApplicationCodeTransformer
import cmu.pasta.sfuzz.runtime.Runtime
import java.lang.instrument.Instrumentation

fun premain(arguments: String?, instrumentation: Instrumentation) {
    println("Premain launched on thread ${Thread.currentThread().threadId()}")
    Runtime.DELEGATE = RuntimeDelegate()
    GlobalContext.registerThread(Thread.currentThread())
    GlobalContext.synchronizationPoints[Thread.currentThread()]?.unblock()
    GlobalContext.currentThreadId = Thread.currentThread().threadId()
    GlobalContext.registerThreadDone(Thread.currentThread())

    // Unblock main thread since Thread.run[main] will be called later
    GlobalContext.registeredThreads[GlobalContext.currentThreadId]?.unblock()


    instrumentation.addTransformer(ApplicationCodeTransformer())
    println("Premain done!")
}