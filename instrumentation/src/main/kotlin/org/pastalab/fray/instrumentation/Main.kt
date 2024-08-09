package org.pastalab.fray.instrumentation

import java.io.File

// --patch-module
// org.pastalab.fray.instrumentation=PATH_TO_SFUZZ/fray/instrumentation/build/classes/kotlin/main
fun main(args: Array<String>) {
  var ba = File(args[0]).inputStream()

  //  instrumentClass(args[0], ba)
  //  val appTransformer = ApplicationCodeTransformer()
  //  appTransformer.transform(null, "", null, null, ba.readBytes())
}
