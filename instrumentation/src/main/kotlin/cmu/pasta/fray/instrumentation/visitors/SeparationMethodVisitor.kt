package cmu.pasta.fray.instrumentation.visitors

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9

class SeparationMethodVisitor(baseMv: MethodVisitor, val wrapperMV: MethodVisitor) :
    MethodVisitor(ASM9, baseMv) {
  override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
    return wrapperMV.visitAnnotation(descriptor, visible)
  }
}
