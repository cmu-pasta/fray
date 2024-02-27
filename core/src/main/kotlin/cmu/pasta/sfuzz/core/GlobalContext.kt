package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.cmu.pasta.sfuzz.core.ThreadContext
import cmu.pasta.sfuzz.cmu.pasta.sfuzz.core.ThreadState
import cmu.pasta.sfuzz.core.concurrency.ReentrantLockMonitor
import cmu.pasta.sfuzz.core.concurrency.SFuzzThread
import cmu.pasta.sfuzz.core.concurrency.Sync
import cmu.pasta.sfuzz.core.concurrency.operations.*
import cmu.pasta.sfuzz.core.scheduler.FifoScheduler
import cmu.pasta.sfuzz.core.scheduler.Scheduler
import java.util.concurrent.locks.ReentrantLock

object GlobalContext {

    val registeredThreads = mutableMapOf<Long, ThreadContext>()
    var currentThreadId: Long = -1;
    private val scheduler: Scheduler = FifoScheduler()
    private val objectWatcher = mutableMapOf<Any, MutableList<Long>>()
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
        registeredThreads[t.threadId()]?.pendingOperation = null
        registeredThreads[t.threadId()]?.state = ThreadState.Enabled
        synchronizationPoints[t]?.unblock() // Thread is enabled
        registeredThreads[t.threadId()]?.block()
    }

    fun threadCompleted(t: Thread) {

        reentrantLockLock(t)
        // Thread.notify is called from JNI, and we don't have
        // instrumentation for it. Therefore, we need to handle
        // object notify here.
        objectNotifyAll(t)
        reentrantLockUnlock(t)

        registeredThreads[t.threadId()]?.state = ThreadState.Completed

        scheduleNextOperation(false)
        println("Thread completed: ${t.threadId()}")
    }

    fun objectWait(o: Any) {
        println("Object wait")
        val t = Thread.currentThread().threadId()
        registeredThreads[t]?.pendingOperation = ObjectWaitOperation()
        scheduleNextOperation(true)

        // If we resume executing, the Object.wait is executed. We should update the
        // state of current thread.
        registeredThreads[t]?.pendingOperation = null
        registeredThreads[t]?.state = ThreadState.Paused
        // We need to unlock the reentrant lock as well.
        reentrantLockMonitor.unlock(o)
        if (o !in objectWatcher) {
            objectWatcher[o] = mutableListOf()
        }
        objectWatcher[o]!!.add(t)
        scheduleNextOperation(true)

        // We are back! We should block until reentrant monitor is released.
        reentrantLockLock(o)
    }

    fun objectNotify(o: Any) {
        println("Object notify")
        registeredThreads[Thread.currentThread().threadId()]?.pendingOperation = ObjectNotifyOperation()
        scheduleNextOperation(true)

        objectWatcher[o]?.let {
            if (it.size > 0) {
                val t = it.removeFirst()
                registeredThreads[t]?.pendingOperation = null
                registeredThreads[t]?.state = ThreadState.Enabled
                it.remove(t)
                if (it.size == 0) {
                    objectWatcher.remove(o)
                }
                scheduleNextOperation(true)
            }
        }
    }

    fun objectNotifyAll(o: Any) {
        println("Object notifyall")
        registeredThreads[Thread.currentThread().threadId()]?.pendingOperation = ObjectNotifyAllOperation()
        scheduleNextOperation(true)

        objectWatcher[o]?.let {
            if (it.size > 0) {
                for (t in it) {
                    registeredThreads[t]?.pendingOperation = null
                    registeredThreads[t]?.state = ThreadState.Enabled
                }
                objectWatcher.remove(o)
                scheduleNextOperation(true)
            }
        }
    }

    fun reentrantLockTrylock(lock: Any) {
        val t = Thread.currentThread().threadId()
        registeredThreads[t]?.pendingOperation = ReentrantLockLockOperation()
        scheduleNextOperation(true)
        reentrantLockMonitor.lock(lock, false)
    }

    fun reentrantLockLock(lock: Any) {
        val t = Thread.currentThread().threadId()
        registeredThreads[t]?.pendingOperation = ReentrantLockLockOperation()
        scheduleNextOperation(true)

        /**
        *  We need a while loop here because even another thread unlock
        *  this thread and makes this thread Enabled. It is still possible
        *  for a third thread to lock it again.
        *  t1 = {
        *    1. foo.lock();
        *    2. foo.unlock();
        *  }
        *  t2 = {
        *    1. foo.lock();
        *    2. foo.unlock();
        *  }
        *  t3 = {
        *     1. foo.lock();
        *     2. foo.unlock();
        *  }
        *  t1.1, t2.1, t1.2, t3.1 will make t2.1 lock again.
        */
        while (!reentrantLockMonitor.lock(lock, true)) {
            registeredThreads[t]?.state = ThreadState.Paused

            // We want to block current thread because we do
            // not want to rely on ReentrantLock. This allows
            // us to pick which Thread to run next if multiple
            // threads hold the same lock.
            scheduleNextOperation(true)
        }
    }

    fun reentrantLockUnlock(lock: Any) {
        val t = Thread.currentThread().threadId()
        registeredThreads[t]?.pendingOperation = ReentrantLockUnlock()
        scheduleNextOperation(true)

        reentrantLockMonitor.unlock(lock)
    }


    fun scheduleNextOperation(shouldBlockCurrentThread: Boolean) {
        // Our current design makes sure that reschedule is only called
        // by scheduled thread.
        assert(currentThreadId == Thread.currentThread().threadId())
        val currentThread = registeredThreads[currentThreadId]
        val nextThread = scheduler.scheduleNextOperation(registeredThreads.values.toList())

        if (nextThread == null) {
            if (registeredThreads.all { it.value.state == ThreadState.Completed }) {
                println("WOW execution finished")
                return
            } else {
                println("Dead lock!")
                return
            }
        }
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