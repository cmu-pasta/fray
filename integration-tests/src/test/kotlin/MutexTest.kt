import MutexTest.t1
import MutexTest.t2
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.sync.Mutex

object MutexTest {

  val mutex = Mutex()

  // We do not support kotlin coroutines right now.
  fun testConcurrentLockUnlock() {}

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
  CoroutineScope(Default).launch { t1() }
  CoroutineScope(Default).launch { t2() }
}
