package org.pastalab.fray.core.controllers

import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.rmi.ThreadState
import org.pastalab.fray.runtime.Runtime
import java.time.Instant
import java.util.TreeSet
import java.util.concurrent.TimeUnit
import kotlin.collections.set

class VirtualTimeController(context: RunContext): TimeControllerInterface(context) {
  val deadlineSet = TreeSet<Long>()
  var running = false
  @Volatile
  var nanoTime = TimeUnit.SECONDS.toNanos(1577768400)
  val timerThread: Thread = Thread {
    while (running) {
      while (deadlineSet.isNotEmpty()) {
        nanoTime += deadlineSet.first()
      }
      Runtime.onSkipPrimitive("timer thread blocked")
      timerThreadContext.state = ThreadState.Blocked
      context.scheduleNextOperation(true)
      Runtime.onSkipPrimitiveDone("timer thread blocked")
    }
  }
  val timerThreadContext: ThreadContext = ThreadContext(timerThread, context.registeredThreads.size, context, -1)

  init {
    context.registeredThreads[timerThread.id] = timerThreadContext
    timerThreadContext.state = ThreadState.Runnable
    timerThread.start()
    context.syncManager.wait(timerThread)
  }

  override fun addDeadline(deadline: Long) {
    if (deadlineSet.isEmpty()) {
      timerThreadContext.state = ThreadState.Runnable
    }
    deadlineSet.add(deadline)
  }

  override fun currentTimeMillis(): Long {
    return nanoTime / 1_000_000
  }

  override fun instantNow(): Instant {
    return Instant.ofEpochMilli(currentTimeMillis())
  }

  override fun nanoTime(): Long {
    return nanoTime
  }

  override fun fastForwardBlockingTime(time: Long) {
    // Do nothing
  }

  override fun done() {

  }
}
