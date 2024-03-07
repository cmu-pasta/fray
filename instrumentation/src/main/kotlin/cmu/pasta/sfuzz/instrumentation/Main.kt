package cmu.pasta.sfuzz.instrumentation

import java.io.File

fun main() {
    var ba = File("/usr0/home/vvikram/Work/sfuzz/examples/build/classes/java/main/example/ArithmeticProgBad.class")
        .readBytes()

    var t = ApplicationCodeTransformer()
    t.transform(null, "", null, null, ba)
}