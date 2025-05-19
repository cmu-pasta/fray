package org.pastalab.fray.core

import java.util.concurrent.locks.ReentrantLock
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.command.ExecutionInfo
import org.pastalab.fray.core.command.InterceptedFeatures
import org.pastalab.fray.core.command.LambdaExecutor
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.scheduler.RandomScheduler
import org.pastalab.fray.rmi.ResourceType

class RunContextReentrantLockTest {
  val context =
      RunContext(
          Configuration(
              ExecutionInfo(
                  LambdaExecutor({}),
                  false,
                  false,
                  -1,
              ),
              "/tmp/fray",
              1,
              1000,
              RandomScheduler(),
              ControlledRandom(),
              false,
              false,
              true,
              false,
              false,
              false,
              InterceptedFeatures.entries.toSet()))

  @BeforeEach
  fun setUp() {
    context.start()
  }

  @Test
  fun testRunContextReentrantLockAcquireAndRelease() {
    val tid = Thread.currentThread().id
    val lock = ReentrantLock()
    val resourceId = System.identityHashCode(lock)
    context.lockLock(lock, false)
    assertEquals(context.registeredThreads[tid]!!.acquiredResources.size, 1)
    val resourceInfo = context.registeredThreads[tid]!!.acquiredResources.first().resourceInfo
    assertEquals(resourceInfo.resourceId, resourceId)
    assertEquals(resourceInfo.resourceType, ResourceType.LOCK)
    context.lockUnlock(lock)
    assertEquals(context.registeredThreads[tid]!!.acquiredResources.size, 0)
  }
}
