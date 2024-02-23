package cmu.pasta.sfuzz.jdk.agent

import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation

fun premain(arguments: String?, instrumentation: Instrumentation) {
}

class Transformer: ClassFileTransformer {
}