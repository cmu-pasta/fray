package cmu.pasta.fray.instrumentation.visitors

import cmu.pasta.fray.runtime.Runtime
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
    val customizer: MethodEnterVisitor.(v: MethodEnterVisitor) -> Unit = {}
) : AdviceAdapter(ASM9, mv, access, name, descriptor) {
  override fun onMethodEnter() {
    super.onMethodEnter()
    if (loadThis) {
      loadThis()
    }
    if (loadArgs) {
      loadArgs()
    }
    customizer(this)
    invokeStatic(
        Type.getObjectType(Runtime::class.java.name.replace(".", "/")),
        Utils.kFunctionToASMMethod(method),
    )
  }
}
