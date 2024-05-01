package cmu.pasta.fray.it.lincheck

import cmu.pasta.fray.core.scheduler.POSScheduler
import cmu.pasta.fray.it.IntegrationTestRunner
import java.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.jupiter.api.Assertions

class MutexTest : IntegrationTestRunner() {

  // We do not support kotlin coroutines right now.
  fun testConcurrentLockUnlock() {
    val res =
        runTest(
            { testConcurrentLockUnlockImpl() },
            POSScheduler(Random()),
            1000,
        )
    Assertions.assertTrue(res.contains("Error found"))
  }

  fun testConcurrentLockUnlockImpl() {
    val mutex = Mutex()
    val thread1 = Thread {
      runBlocking {
        mutex.lock()
        mutex.unlock()
      }
    }
    val thread2 = Thread {
      runBlocking {
        mutex.lock()
        mutex.lock()
      }
    }
    thread1.start()
    thread2.start()
    thread1.join()
    thread2.join()
  }
}