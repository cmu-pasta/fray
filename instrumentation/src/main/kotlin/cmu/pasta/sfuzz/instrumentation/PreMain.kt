package cmu.pasta.sfuzz.instrumentation

import java.lang.instrument.Instrumentation

fun premain(arguments: String?, instrumentation: Instrumentation) {
    Utils.prepareDebugFolder("app")
    instrumentation.addTransformer(ApplicationCodeTransformer())
}