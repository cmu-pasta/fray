package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.concurrency.ReentrantLockMonitor
import cmu.pasta.sfuzz.core.concurrency.SFuzzThread
import cmu.pasta.sfuzz.core.concurrency.Sync
import cmu.pasta.sfuzz.core.concurrency.operations.*
import cmu.pasta.sfuzz.core.logger.LoggerBase
import cmu.pasta.sfuzz.core.runtime.AnalysisResult
import cmu.pasta.sfuzz.core.scheduler.Choice
import cmu.pasta.sfuzz.core.scheduler.FifoScheduler
import cmu.pasta.sfuzz.core.scheduler.Scheduler
import cmu.pasta.sfuzz.instrumentation.memory.VolatileManager
import cmu.pasta.sfuzz.runtime.Delegate
import cmu.pasta.sfuzz.runtime.MemoryOpType
import cmu.pasta.sfuzz.runtime.Runtime
import cmu.pasta.sfuzz.runtime.TargetTerminateException
import jdk.jfr.Enabled
import java.util.concurrent.Executors
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

// TODO(aoli): make this a class maybe?
object GlobalContext {
    val registeredThreads = mutableMapOf<Long, ThreadContext>()
    var currentThreadId: Long = -1;
    var scheduler: Scheduler = FifoScheduler()
    var config: Configuration? = null
    private val objectWatcher = mutableMapOf<Any, MutableList<Long>>()
    private val reentrantLockMonitor = ReentrantLockMonitor()
    private val volatileManager = VolatileManager()
    val loggers = mutableListOf<LoggerBase>()
    val synchronizationPoints = mutableMapOf<Any, Sync>()
    var executor = Executors.newSingleThreadExecutor { r ->
        object : SFuzzThread() {
            override fun run() {
                r.run()
            }
        }
    }

    fun bootStrap() {
        executor = Executors.newSingleThreadExecutor { r ->
            object : SFuzzThread() {
                override fun run() {
                    r.run()
                }
            }
        }
    }

    fun start() {
        val t = Thread.currentThread();
        // We need to submit a dummy task to trigger the executor
        // thread creation
        executor.submit {}
        currentThreadId = t.id
        registeredThreads[t.id] = ThreadContext(t, registeredThreads.size)
        registeredThreads[t.id]?.state = ThreadState.Enabled
        loggers.forEach {
            it.executionStart()
        }
        scheduleNextOperation(true)
    }

    fun done(result: AnalysisResult) {
        loggers.forEach {
            it.executionDone(result)
        }

        reentrantLockMonitor.done()
        assert(objectWatcher.isEmpty())
        assert(synchronizationPoints.isEmpty())
        registeredThreads.clear()
    }

    fun shutDown() {
        Runtime.DELEGATE = Delegate()
        executor.shutdown()
    }

    fun registerLogger(l: LoggerBase) {
        loggers.add(l)
    }

    fun threadStart(t: Thread) {
        registeredThreads[t.id] = ThreadContext(t, registeredThreads.size)
        synchronizationPoints[t] = Sync(1)
    }

    fun threadStartDone(t: Thread) {
        // Wait for the new thread runs.
        synchronizationPoints[t]?.block()
        synchronizationPoints.remove(t)
    }

    fun threadPark() {
        val t = Thread.currentThread()
        if (!registeredThreads[t.id]!!.unparkSignaled) {
            registeredThreads[t.id]?.pendingOperation = PausedOperation()
            registeredThreads[t.id]?.state = ThreadState.Parked
            scheduleNextOperation(false)
        } else {
            registeredThreads[t.id]!!.unparkSignaled = false
        }
    }

    fun threadParkDone() {
        val t = Thread.currentThread()
        val context = registeredThreads[t.id]!!
        // If the thread is still running, it means
        // that the thread is unparked before it is parked.
        if (context.state == ThreadState.Running) {
            return
        }
        assert(context.state == ThreadState.Parked)
        synchronizationPoints[t]?.unblock()
        context.block()
    }

    fun threadUnpark(t: Thread) {
        val context = registeredThreads[t.id]!!
        if (context.state != ThreadState.Parked) {
            context.unparkSignaled = true
        } else {
            synchronizationPoints[t] = Sync(1)
            context.state = ThreadState.Enabled
            registeredThreads[t.id]?.pendingOperation = ThreadResumeOperation()
        }
    }

    fun threadUnparkDone(t: Thread) {
        if (registeredThreads[t.id]!!.state == ThreadState.Parked) {
            // SFuzz only needs to wait if `t` is parked and then
            // waken up by this `unpark` operation.
            synchronizationPoints[t]?.block()
            synchronizationPoints.remove(t)
        }
    }

    fun threadRun() {
        var t = Thread.currentThread()
        registeredThreads[t.id]?.pendingOperation = ThreadStartOperation()
        registeredThreads[t.id]?.state = ThreadState.Enabled
        synchronizationPoints[t]?.unblock() // Thread is enabled
        registeredThreads[t.id]?.block()
    }

    fun threadCompleted(t: Thread) {
        objectNotifyAll(t)
        registeredThreads[t.id]?.state = ThreadState.Completed
        // We do not want to send notify all because
        // we don't have monitor lock here.
        reentrantLockUnlock(t, false)
        executor.submit {
            while (t.isAlive) {
                Thread.yield()
            }
            reentrantLockUnlockDone(t)
            scheduleNextOperation(false)
        }
    }

    fun objectWait(o: Any) {
        val t = Thread.currentThread().id
        val objId = System.identityHashCode(o)
        registeredThreads[t]?.pendingOperation = ObjectWaitOperation(objId)
        registeredThreads[t]?.state = ThreadState.Enabled
        scheduleNextOperation(true)
        // If we resume executing, the Object.wait is executed. We should update the
        // state of current thread.
        registeredThreads[t]?.pendingOperation = PausedOperation()
        registeredThreads[t]?.state = ThreadState.Paused

        if (o !in objectWatcher) {
            objectWatcher[o] = mutableListOf()
        }
        objectWatcher[o]!!.add(t)
        reentrantLockUnlock(o, true)

        // We need a daemon thread here because
        // `object.wait` release the monitor lock implicitly.
        // Therefore, we need to call `reentrantLockUnlockDone`
        // manually.
        executor.submit {
            reentrantLockUnlockDone(o)
            scheduleNextOperation(false)
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    fun objectWaitDone(o: Any) {
        val t = Thread.currentThread()
        val context = registeredThreads[t.id]!!
        // We will unblock here only if the scheduler
        // decides to run it.
        while (context.state != ThreadState.Running) {
            synchronizationPoints[o]!!.unblock()
            synchronized(o) {
                (o as Object).wait()
            }
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    fun objectNotify(o: Any) {
        objectWatcher[o]?.let {
            if (it.size > 0) {
                val t = it.removeFirst()
                val context = registeredThreads[t]!!
                reentrantLockMonitor.addWaiter(o, context.thread)
                context.waitsOn = o
                it.remove(t)
                if (it.size == 0) {
                    objectWatcher.remove(o)
                }
            }
        }
    }

    fun objectNotifyAll(o: Any) {
        objectWatcher[o]?.let {
            if (it.size > 0) {
                for (t in it) {
                    val context = registeredThreads[t]!!
                    // We cannot enable the thread immediately because
                    // the thread is still waiting for the monitor lock.
                    context.waitsOn = o
                    reentrantLockMonitor.addWaiter(o, context.thread)
                }
                objectWatcher.remove(o)
            }
        }
    }

    fun reentrantLockTrylock(lock: Any) {
        val t = Thread.currentThread().id
        val objId = System.identityHashCode(lock)
        registeredThreads[t]?.pendingOperation = ReentrantLockLockOperation(objId)
        registeredThreads[t]?.state = ThreadState.Enabled
        scheduleNextOperation(true)
        reentrantLockMonitor.lock(lock, t, false)
    }

    fun reentrantLockLock(lock: Any) {
        val t = Thread.currentThread().id
        val objId = System.identityHashCode(lock)
        registeredThreads[t]?.pendingOperation = ReentrantLockLockOperation(objId)
        registeredThreads[t]?.state = ThreadState.Enabled
        scheduleNextOperation(true)

        /**
        *  We need a while loop here because even a thread unlock
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
        while (!reentrantLockMonitor.lock(lock, t, true)) {
            registeredThreads[t]?.state = ThreadState.Paused

            // We want to block current thread because we do
            // not want to rely on ReentrantLock. This allows
            // us to pick which Thread to run next if multiple
            // threads hold the same lock.
            scheduleNextOperation(true)
        }
    }

    fun log(format: String, vararg args: Any) {
        val tid = Thread.currentThread().id
        val context = registeredThreads[tid]!!
        val data = "[${context.index}]: ${String.format(format, args)}"
        for (logger in loggers) {
            logger.applicationEvent(data)
        }
    }

    fun reentrantLockUnlock(lock: Any, sendNotifyAll: Boolean) {
        val waitingThreads = reentrantLockMonitor.unlock(lock)
        if (waitingThreads > 0) {
            if (sendNotifyAll) {
                synchronized(lock) {
                    // Make some noise to wake up all waiting threads.
                    // This also ensure that the previous `notify` `notifyAll`
                    // are treated as no-ops.
                    (lock as Object).notifyAll()
                }
            }
            synchronizationPoints[lock] = Sync(waitingThreads)
        }
    }

    fun reentrantLockUnlockDone(lock: Any) {
        synchronizationPoints[lock]?.block()
        synchronizationPoints.remove(lock)
    }

    fun fieldOperation(obj: Any?, owner: String, name: String, type: MemoryOpType) {
        if (!volatileManager.isVolatile(owner, name)) return
        val objIds = mutableListOf<Int>()
        if (obj != null) {
            objIds.add(System.identityHashCode(obj))
        } else {
            objIds.add(owner.hashCode())
        }
        objIds.add(name.hashCode())
        memoryOperation(objIds.toIntArray().contentHashCode(), type)
    }

    fun atomicOperation(obj: Any, type: MemoryOpType) {
        val objId = System.identityHashCode(obj)
        memoryOperation(objId, type)
    }

    fun memoryOperation(obj: Int, type: MemoryOpType) {
        val t = Thread.currentThread().id
        registeredThreads[t]?.pendingOperation = MemoryOperation(obj, type)
        registeredThreads[t]?.state = ThreadState.Enabled
        scheduleNextOperation(true)
    }

    fun scheduleNextOperation(shouldBlockCurrentThread: Boolean) {
        // Our current design makes sure that reschedule is only called
        // by scheduled thread.
        assert(Thread.currentThread() is SFuzzThread || currentThreadId == Thread.currentThread().id)
        val currentThread = registeredThreads[currentThreadId]!!
        assert(currentThread.state == ThreadState.Enabled || currentThread.state == ThreadState.Completed)
        val enabledOperations = registeredThreads.values.toList()
            .filter { it.state == ThreadState.Enabled }
            .sortedBy { it.thread.id }

        if (enabledOperations.isEmpty()) {
            if (registeredThreads.all { it.value.state == ThreadState.Completed }) {
                // We are done here, we should go back to the
                return
            } else {
                // Deadlock detected
                throw TargetTerminateException(-1)
            }
        }
        val nextThread = scheduler.scheduleNextOperation(enabledOperations)
        val index = enabledOperations.indexOf(nextThread)
        currentThreadId = nextThread.thread.id

        if (enabledOperations.size > 1 || config!!.fullSchedule) {
            loggers.forEach {
                it.newOperationScheduled(currentThread.pendingOperation!!,
                    Choice(index, currentThread.index, enabledOperations.size))
            }
        }
        nextThread.state = ThreadState.Running
        if (currentThread != nextThread) {
            unblockThread(nextThread)
            if (shouldBlockCurrentThread) {
                currentThread.block()
            }
        }
    }

    fun unblockThread(t: ThreadContext) {
        // If this object is blocked through JDK locks,
        // the thread is waiting for monitor locks.
        // We first need to give the thread lock
        // and then wakes it up through `notifyAll`.
        if (t.waitsOn != null) {
            // If a thread is enabled, the lock must be available.
            assert(reentrantLockMonitor.lock(t.waitsOn!!, t.thread.id, false))
            synchronized(t.waitsOn!!) {
                (t.waitsOn as Object).notifyAll()
            }
            t.waitsOn = null
        } else {
            t.unblock()
        }
    }
}