package org.pastalab.fray.instrumentation.base.visitors

import kotlin.reflect.KFunction
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

class MethodEnterVisitor(
    mv: MethodVisitor,
    val method: KFunction<*>,
    access: Int,
    name: String,
    descriptor: String,
    val loadThis: Boolean,
    val loadArgs: Boolean,
    val preCustomizer: MethodEnterVisitor.(v: MethodEnterVisitor) -> Unit = {},
    val postCustomizer: MethodEnterVisitor.(v: MethodEnterVisitor) -> Unit = {},
) : AdviceAdapter(ASM9, mv, access, name, descriptor) {
  override fun onMethodEnter() {
    super.onMethodEnter()
    if (loadThis) {
      loadThis()
    }
    if (loadArgs) {
      loadArgs()
    }
    preCustomizer(this)
    invokeStatic(
        Type.getObjectType(org.pastalab.fray.runtime.Runtime::class.java.name.replace(".", "/")),
        Utils.kFunctionToASMMethod(method),
    )
    postCustomizer(this)
  }
}
