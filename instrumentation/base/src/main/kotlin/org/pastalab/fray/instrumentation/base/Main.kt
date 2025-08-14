package org.pastalab.fray.instrumentation.base

import java.io.File

// --patch-module
// org.pastalab.fray.instrumentation=PATH_TO_SFUZZ/fray/instrumentation/build/classes/kotlin/main
fun main(args: Array<String>) {
  val ba = File(args[0]).inputStream()

  //  val byteArray = ba.readBytes()
  val appTransformer = ApplicationCodeTransformer()
  appTransformer.transform(null, "", null, null, ba.readBytes())
  //  val classReader = ClassReader(byteArray)
  //  val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
  //  val cn = ClassNode()
  //  classReader.accept(cn, ClassReader.EXPAND_FRAMES)
  //  cn.accept(CheckClassAdapter(classWriter))
}
