package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.cmu.pasta.sfuzz.core.ThreadContext
import cmu.pasta.sfuzz.cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.concurrency.Sync
import cmu.pasta.sfuzz.core.scheduler.FifoScheduler
import cmu.pasta.sfuzz.core.scheduler.Scheduler
import java.util.concurrent.CountDownLatch

object GlobalContext {

    private val registeredThreads = mutableMapOf<Long, ThreadContext>()
    var currentThreadId: Long = -1;
    private val scheduler: Scheduler = FifoScheduler()
    private val objectWatcher = mutableMapOf<Any, Set<Thread>>()
    private val synchronizationPoints = mutableMapOf<Any, Sync>()

    fun registerThread(t: Thread) {
        println("New thread registered ${t.threadId()}")
        registeredThreads[t.threadId()] = ThreadContext(t)
        synchronizationPoints[t] = Sync(true)
    }

    fun registerThreadDone(t: Thread) {
        // Wait for the new thread runs.
        synchronizationPoints[t]?.block()
        synchronizationPoints.remove(t)
        scheduleNextOperation(true)
    }

    fun onThreadRun() {
        var t = Thread.currentThread()
        registeredThreads[t.threadId()]?.state = ThreadState.Enabled
        synchronizationPoints[t]?.unblock() // Thread is enabled
        registeredThreads[t.threadId()]?.block()
    }

    fun threadCompleted(t: Thread) {
        registeredThreads[t.threadId()]?.state = ThreadState.Completed
        // Thread is completed so we don't need to block it!
        scheduleNextOperation(false)
    }

    fun objectWait(t: Any) {
        registeredThreads[Thread.currentThread().threadId()]?.state = ThreadState.Paused
        // We should not pause current thread because Object.wait will block the execution.
        // It is safe to reschedule here because `Object.wait` is always guarded with sync block.
        scheduleNextOperation(false)
    }

    fun objectWaitDone(t: Any) {
        registeredThreads[Thread.currentThread().threadId()]?.state = ThreadState.Enabled
        synchronizationPoints[t]?.unblock()
        registeredThreads[Thread.currentThread().threadId()]?.block()
    }

    fun objectNotify(t: Any) {
        synchronizationPoints[t] = Sync(true)
    }

    fun objectNotifyDone(t: Any) {
        // Wait for the new thread block again.
        synchronizationPoints[t]?.block()
        scheduleNextOperation(true)
    }

    fun scheduleNextOperation(shouldBlockCurrentThread: Boolean) {
        // Our current design makes sure that reschedule is only called
        // by scheduled thread.
        assert(currentThreadId == Thread.currentThread().threadId())
        val currentThread = registeredThreads[currentThreadId]
        val nextThread = scheduler.scheduleNextOperation(registeredThreads.values.toList())
        currentThreadId = nextThread.thread.threadId()
        println("New thread scheduled $currentThreadId")
        if (currentThread != nextThread) {
            nextThread.unblock()
        }
        if (shouldBlockCurrentThread) {
            currentThread?.block()
        }
    }
}