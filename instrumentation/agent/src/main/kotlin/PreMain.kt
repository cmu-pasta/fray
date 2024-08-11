package org.pastalab.fray.instrumentation.agent

import java.lang.instrument.Instrumentation

fun premain(arguments: String?, instrumentation: Instrumentation) {
  instrumentation.addTransformer(ApplicationCodeTransformer())
}
