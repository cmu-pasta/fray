package visitors

import cmu.pasta.sfuzz.instrumentation.visitors.Utils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

fun exampleMethod(param1: String, param2: Int): Long = 42L
class UtilsTest {

    @Test
    fun kFunctionToJvmMethodDescriptor() {
        assertEquals("(Ljava/lang/String;I)J", Utils.kFunctionToJvmMethodDescriptor(::exampleMethod))
    }

//    @Test
//    fun kFunctionToJvmMethodDescriptorWithStaticMethod() {
//        assertEquals("()V", Utils.kFunctionToJvmMethodDescriptor(Runtime::onThreadRun))
//    }
}