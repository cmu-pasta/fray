package cmu.pasta.sfuzz.instrumentation

import java.io.File

fun main() {
    var ba = File("/home/aoli/repos/sfuzz/benchmarks/build/libs/unzipped/dacapo-23.11-chopin/jar/lib/lucene/org/apache/lucene/index/IndexWriter.class")
        .readBytes()

    var t = ApplicationCodeTransformer()
    t.transform(null, "", null, null, ba)
}