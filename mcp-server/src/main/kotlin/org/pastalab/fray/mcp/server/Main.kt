package org.pastalab.fray.mcp.server

class Foo {}

fun main() {
  println(Foo::class.java.protectionDomain.codeSource)
  val clazz = Foo::class.java
  Thread.currentThread().stackTrace.forEach { println(it.fileName.toString()) }
}
