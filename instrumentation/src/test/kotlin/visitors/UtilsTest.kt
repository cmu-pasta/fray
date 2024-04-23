package visitors

import cmu.pasta.fray.instrumentation.visitors.Utils
import cmu.pasta.fray.runtime.Runtime
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
        Utils.kFunctionToJvmMethodDescriptor(Runtime::onSemaphoreAcquirePermits))
  }
}
