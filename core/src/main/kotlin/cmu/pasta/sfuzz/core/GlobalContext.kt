package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.cmu.pasta.sfuzz.core.ThreadContext
import cmu.pasta.sfuzz.cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.concurrency.ReentrantLockMonitor
import cmu.pasta.sfuzz.core.concurrency.SFuzzThread
import cmu.pasta.sfuzz.core.concurrency.Sync
import cmu.pasta.sfuzz.core.concurrency.operations.ObjectNotifyAllOperation
import cmu.pasta.sfuzz.core.concurrency.operations.ObjectNotifyOperation
import cmu.pasta.sfuzz.core.concurrency.operations.ObjectWaitOperation
import cmu.pasta.sfuzz.core.scheduler.FifoScheduler
import cmu.pasta.sfuzz.core.scheduler.Scheduler
import java.util.concurrent.locks.ReentrantLock

object GlobalContext {

    val registeredThreads = mutableMapOf<Long, ThreadContext>()
    var currentThreadId: Long = -1;
    private val scheduler: Scheduler = FifoScheduler()
    private val objectWatcher = mutableMapOf<Any, MutableSet<Thread>>()
    private val reentrantLockMonitor = ReentrantLockMonitor()
    val synchronizationPoints = mutableMapOf<Any, Sync>()

    fun threadStart(t: Thread) {
        println("New thread registered ${t.threadId()}")
        registeredThreads[t.threadId()] = ThreadContext(t)
        synchronizationPoints[t] = Sync(1)
    }

    fun threadStartDone(t: Thread) {
        println("Thread registration done ${t.threadId()}")
        // Wait for the new thread runs.
        synchronizationPoints[t]?.block()
        synchronizationPoints.remove(t)
//        scheduleNextOperation(true)
    }

    fun threadRun() {
        var t = Thread.currentThread()
        println("Thread run: ${t.threadId()}")
        registeredThreads[t.threadId()]?.state = ThreadState.Enabled
        synchronizationPoints[t]?.unblock() // Thread is enabled
        registeredThreads[t.threadId()]?.block()
    }

    fun threadCompleted(t: Thread) {
        println("Thread completed: ${t.threadId()}")
        registeredThreads[t.threadId()]?.state = ThreadState.Completed

        // Thread.notify is called from JNI, and we don't have
        // instrumentation for it. Therefore, we need to handle
        // object notify here. Our solution is to create a daemon
        // thread that mimic the notification behaviour.
        objectNotify(t)
        var t = object: SFuzzThread() {
            override fun run() {
                objectNotifyDone(t)
                // We should never block a helper thread.
                scheduleNextOperation(false)
            }
        }
        t.isDaemon = true
        t.start()
    }

    fun objectWait(t: Any) {
        println("Object wait")

        registeredThreads[Thread.currentThread().threadId()]?.pendingOperation = ObjectWaitOperation()
        scheduleNextOperation(true)


        // If we resume executing, the Object.wait is executed. We should update the
        // state of current thread.
        registeredThreads[Thread.currentThread().threadId()]?.pendingOperation = null
        registeredThreads[Thread.currentThread().threadId()]?.state = ThreadState.Paused
        if (t !in objectWatcher) {
            objectWatcher[t] = mutableSetOf()
        }
        objectWatcher[t]!!.add(Thread.currentThread())

        // We should not pause current thread because Object.wait will block the execution.
        // It is safe to reschedule here because `Object.wait` is always guarded with sync block.
        scheduleNextOperation(false)
    }

    fun objectWaitDone(t: Any) {
        println("Object wait done")
        registeredThreads[Thread.currentThread().threadId()]?.pendingOperation = null
        registeredThreads[Thread.currentThread().threadId()]?.state = ThreadState.Enabled
        objectWatcher[t]?.remove(Thread.currentThread())
        if (objectWatcher[t]?.size == 0) {
            objectWatcher.remove(t)
        }
        synchronizationPoints[t]?.unblock()
        registeredThreads[Thread.currentThread().threadId()]?.block()
    }

    fun objectNotify(t: Any) {
        println("Object notify")
        registeredThreads[Thread.currentThread().threadId()]?.pendingOperation = ObjectNotifyOperation()
        scheduleNextOperation(true)
        objectWatcher[t]?.size?.let {
            if (it > 0) {
                synchronizationPoints[t] = Sync(1)
            }
        }
    }

    fun objectNotifyAll(t: Any) {
        println("Object notifyall")
        registeredThreads[Thread.currentThread().threadId()]?.pendingOperation = ObjectNotifyAllOperation()
        scheduleNextOperation(true)
        objectWatcher[t]?.size?.let {
            if (it > 0) {
                synchronizationPoints[t] = Sync(it)
            }
        }
    }

    fun objectNotifyDone(t: Any) {
        println("Object notify done")
        // Wait for the new thread block again.
        synchronizationPoints[t]?.block()
        synchronizationPoints.remove(t)
    }

    fun reentrantLockLock(lock: ReentrantLock) {
        println("Reentrant lock")
        // It seems that for locks, we only need to
        // reschedule after the lock is acquired. We do
        // not need to reschedule before it.
        if (!reentrantLockMonitor.lock(lock)) {
            registeredThreads[Thread.currentThread().threadId()]?.state = ThreadState.Paused

            // We want to block current thread because we do
            // not want to rely on ReentrantLock. This allows
            // us to pick which Thread to run next if multiple
            // threads hold the same lock.
            scheduleNextOperation(true)
        }
    }

    fun reentrantLockLockDone(lock: ReentrantLock) {
        println("Reentrant lock done")

        // Current thread acquires the ReentrantLock. Reschedule.
        scheduleNextOperation(true)
    }

    fun reentrantLockUnlock(lock: ReentrantLock) {
        // We should pick one
    }


    fun scheduleNextOperation(shouldBlockCurrentThread: Boolean) {
        // Our current design makes sure that reschedule is only called
        // by scheduled thread.
        assert(currentThreadId == Thread.currentThread().threadId())
        val currentThread = registeredThreads[currentThreadId]
        val nextThread = scheduler.scheduleNextOperation(registeredThreads.values.toList())
            ?: throw RuntimeException("Dead lock!")
        currentThreadId = nextThread.thread.threadId()
        println("New thread scheduled $currentThreadId")
        if (currentThread != nextThread) {
            nextThread.unblock()
            if (shouldBlockCurrentThread) {
                currentThread?.block()
            }
        }
    }
}