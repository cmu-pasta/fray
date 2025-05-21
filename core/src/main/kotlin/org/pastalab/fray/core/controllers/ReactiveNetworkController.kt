package org.pastalab.fray.core.controllers

import java.nio.channels.Selector
import java.nio.channels.spi.AbstractInterruptibleChannel
import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.concurrency.context.ReactiveSocketBlocked
import org.pastalab.fray.core.concurrency.operations.ReactiveSelectorBlocked
import org.pastalab.fray.rmi.ThreadState

class ReactiveNetworkController(val runContext: RunContext) : RunFinishedHandler(runContext) {
  fun selectorBlocked(selector: Selector) {
    val tid = Thread.currentThread().id
    val threadContext = runContext.registeredThreads[tid]!!
    threadContext.state = ThreadState.Blocked
    threadContext.pendingOperation = ReactiveSelectorBlocked(selector)
    runContext.reactiveBlockedThreadQueue.add(tid)
    runContext.executor.submit { runContext.scheduleNextOperation(false) }
  }

  fun selectorSelectDone() {
    val threadContext = runContext.registeredThreads[Thread.currentThread().id]!!
    synchronized(runContext.reactiveResumedThreadQueue) {
      runContext.reactiveResumedThreadQueue.add(Thread.currentThread().id)
      (runContext.reactiveResumedThreadQueue as Object).notify()
    }
    threadContext.block()
  }

  fun socketChannelBlocked(channel: AbstractInterruptibleChannel) {
    val tid = Thread.currentThread().id
    val threadContext = runContext.registeredThreads[tid]!!
    threadContext.state = ThreadState.Blocked
    threadContext.pendingOperation = ReactiveSocketBlocked(channel)
    runContext.reactiveBlockedThreadQueue.add(tid)
    runContext.executor.submit { runContext.scheduleNextOperation(false) }
  }

  fun socketChannelBlockedDone() {
    val threadContext = runContext.registeredThreads[Thread.currentThread().id]!!
    synchronized(runContext.reactiveResumedThreadQueue) {
      runContext.reactiveResumedThreadQueue.add(Thread.currentThread().id)
      (runContext.reactiveResumedThreadQueue as Object).notify()
    }
    threadContext.block()
  }

  override fun done() {}
}
