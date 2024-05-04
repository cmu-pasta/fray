package cmu.pasta.fray.instrumentation.visitors

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9

class ClassVersionInstrumenter(cv: ClassVisitor) : ClassVisitor(ASM9, cv) {
  override fun visit(
      version: Int,
      access: Int,
      name: String?,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?
  ) {
    val majorVersion = version and 0xFF
    val patchedVersion =
        if (majorVersion < Opcodes.V1_5) {
          Opcodes.V1_5
        } else {
          majorVersion
        } + (version shr 16 shl 16)
    super.visit(patchedVersion, access, name, signature, superName, interfaces)
  }
}
