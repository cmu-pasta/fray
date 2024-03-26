package cmu.pasta.sfuzz.instrumentation

import java.io.File

fun main(args: Array<String>) {
  var ba = File(args[0]).inputStream()

  instrumentClass(args[0], ba)
  //    var t = ApplicationCodeTransformer()
  //    t.transform(null, "", null, null, ba)
}
