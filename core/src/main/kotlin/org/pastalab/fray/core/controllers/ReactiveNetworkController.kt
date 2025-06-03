package org.pastalab.fray.core.controllers

import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.concurrency.operations.ReactiveBlockingBlocked
import org.pastalab.fray.core.concurrency.operations.ReactiveBlockingOperation
import org.pastalab.fray.core.utils.Utils.verifyNoThrow
import org.pastalab.fray.rmi.ThreadState

class ReactiveNetworkController(val runContext: RunContext) : RunFinishedHandler(runContext) {
  fun reactiveBlockingUnblocked() = verifyNoThrow {
    val threadContext = runContext.registeredThreads[Thread.currentThread().id]!!
    synchronized(runContext.reactiveResumedThreadQueue) {
      runContext.reactiveResumedThreadQueue.add(Thread.currentThread().id)
      (runContext.reactiveResumedThreadQueue as Object).notify()
    }
    threadContext.block()
  }

  fun reactiveBlockingBlocked() = verifyNoThrow {
    val tid = Thread.currentThread().id
    val threadContext = runContext.registeredThreads[tid]!!

    // Reschedule before making network call
    threadContext.pendingOperation = ReactiveBlockingOperation()
    threadContext.state = ThreadState.Runnable
    runContext.scheduleNextOperation(true)

    threadContext.state = ThreadState.Blocked
    threadContext.pendingOperation = ReactiveBlockingBlocked(threadContext)
    runContext.reactiveBlockedThreadQueue.add(tid)
    runContext.executor.submit { runContext.scheduleNextOperation(false) }
  }

  override fun done() {}
}
