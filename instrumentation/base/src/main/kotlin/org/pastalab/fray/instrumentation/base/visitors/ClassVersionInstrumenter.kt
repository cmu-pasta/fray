package org.pastalab.fray.instrumentation.base.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9

class ClassVersionInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  var classVersion = Opcodes.V21

  override fun visit(
      version: Int,
      access: Int,
      name: String?,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?,
  ) {
    classVersion = version and 0xFF
    val patchedVersion =
        if (classVersion < Opcodes.V1_5) {
          Opcodes.V1_5
        } else {
          classVersion
        } + (version shr 16 shl 16)
    super.visit(patchedVersion, access, name, signature, superName, interfaces)
  }
}
