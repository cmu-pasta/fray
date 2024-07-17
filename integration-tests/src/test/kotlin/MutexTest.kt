import cmu.pasta.fray.core.scheduler.POSScheduler
import cmu.pasta.fray.it.IntegrationTestRunner
import MutexTest.t1
import MutexTest.t2
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.sync.Mutex
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

object MutexTest : IntegrationTestRunner() {

  val mutex = Mutex()

  // We do not support kotlin coroutines right now.
  @Test
  fun testConcurrentLockUnlock() {
    val res =
        runTest(
            { main() },
            POSScheduler(Random()),
            1000,
        )
    Assertions.assertTrue(res.contains("Error found"))
  }

  suspend fun t1() {
    mutex.lock()
    mutex.unlock()
  }

  suspend fun t2() {
    mutex.lock()
    mutex.lock()
  }

}

fun main() {
  CoroutineScope(Default).launch {
    t1()
  }
  CoroutineScope(Default).launch {
    t2()
  }
}
