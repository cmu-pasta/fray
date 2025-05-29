package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter

fun GeneratorAdapter.getLocals(): Array<Any> {
  val argTypes = argumentTypes.map { typeToLocal(it) }.toMutableList()
  if (access and Opcodes.ACC_STATIC == 0) {
    argTypes.add(0, "java/lang/Object")
  }
  return argTypes.toTypedArray()
}

fun typeToLocal(type: Type): Any {
  return when (type.sort) {
    Type.BOOLEAN,
    Type.CHAR,
    Type.BYTE,
    Type.SHORT,
    Type.INT -> Opcodes.INTEGER
    Type.FLOAT -> Opcodes.FLOAT
    Type.LONG -> Opcodes.LONG
    Type.DOUBLE -> Opcodes.DOUBLE
    Type.ARRAY -> type.descriptor
    Type.OBJECT -> type.internalName
    else -> throw AssertionError()
  }
}
