package org.pastalab.fray.instrumentation.agent

import java.lang.instrument.Instrumentation
import org.pastalab.fray.instrumentation.base.ApplicationCodeTransformer

fun premain(arguments: String?, instrumentation: Instrumentation) {
  instrumentation.addTransformer(ApplicationCodeTransformer())
}
