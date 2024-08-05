package org.pastalab.fray.instrumentation

import java.lang.instrument.Instrumentation

fun premain(arguments: String?, instrumentation: Instrumentation) {
  //  Utils.prepareDebugFolder("app")
  //  Utils.prepareDebugFolder("origin")
  instrumentation.addTransformer(ApplicationCodeTransformer())
}
