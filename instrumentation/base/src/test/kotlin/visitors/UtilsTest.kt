package visitors

import org.anonlab.fray.instrumentation.base.visitors.Utils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

fun exampleMethod(param1: String, param2: Int): Long = 42L

class UtilsTest {

  @Test
  fun kFunctionToJvmMethodDescriptor() {
    assertEquals("(Ljava/lang/String;I)J", Utils.kFunctionToJvmMethodDescriptor(::exampleMethod))
  }

  @Test
  fun kFunctionToJvmMethodDescriptorWithStaticMethod() {
    assertEquals(
        "(Ljava/util/concurrent/Semaphore;I)V",
        Utils.kFunctionToJvmMethodDescriptor(
            org.anonlab.fray.runtime.Runtime::onSemaphoreAcquirePermits))
  }
}
