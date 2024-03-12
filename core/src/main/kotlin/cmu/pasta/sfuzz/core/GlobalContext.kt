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
import java.util.concurrent.Executors

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
        }
        registeredThreads[t.id]!!.unparkSignaled = false
    }

    fun threadUnpark(t: Thread) {
        if (re)
    }

    fun threadRun() {
        var t = Thread.currentThread()
        registeredThreads[t.id]?.pendingOperation = ThreadStartOperation()
        registeredThreads[t.id]?.state = ThreadState.Enabled
        synchronizationPoints[t]?.unblock() // Thread is enabled
        registeredThreads[t.id]?.block()
    }

    fun threadCompleted(t: Thread) {
        reentrantLockLock(t)
        // Thread.notify is called from JNI, and we don't have
        // instrumentation for it. Therefore, we need to handle
        // object notify here.
        objectNotifyAll(t)
        reentrantLockUnlock(t)
        registeredThreads[t.id]?.state = ThreadState.Completed
        executor.submit {
            while (t.isAlive) {
                Thread.yield()
            }
            scheduleNextOperation(false)
        }
    }

    fun objectWait(o: Any) {
        val t = Thread.currentThread().id
        val objId = System.identityHashCode(o)
        registeredThreads[t]?.pendingOperation = ObjectWaitOperation(objId)
        scheduleNextOperation(true)
        // If we resume executing, the Object.wait is executed. We should update the
        // state of current thread.
        registeredThreads[t]?.pendingOperation = PausedOperation()
        registeredThreads[t]?.state = ThreadState.Paused

        // We need to unlock the reentrant lock as well.
        // Unlock and wait is an atomic operation, we should not
        // reschedule here.
        reentrantLockUnlock(o)
        if (o !in objectWatcher) {
            objectWatcher[o] = mutableListOf()
        }
        objectWatcher[o]!!.add(t)
        scheduleNextOperation(true)

        // We are back! We should block until reentrant monitor is released.
        reentrantLockLock(o)
    }

    fun objectNotify(o: Any) {
        objectWatcher[o]?.let {
            if (it.size > 0) {
                val t = it.removeFirst()
                registeredThreads[t]?.pendingOperation = ThreadResumeOperation()
                registeredThreads[t]?.state = ThreadState.Enabled
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
                    registeredThreads[t]?.pendingOperation = ThreadResumeOperation()
                    registeredThreads[t]?.state = ThreadState.Enabled
                }
                objectWatcher.remove(o)
            }
        }
    }

    fun reentrantLockTrylock(lock: Any) {
        val t = Thread.currentThread().id
        val objId = System.identityHashCode(lock)
        registeredThreads[t]?.pendingOperation = ReentrantLockLockOperation(objId)
        scheduleNextOperation(true)
        reentrantLockMonitor.lock(lock, false)
    }

    fun reentrantLockLock(lock: Any) {
        val t = Thread.currentThread().id
        val objId = System.identityHashCode(lock)
        registeredThreads[t]?.pendingOperation = ReentrantLockLockOperation(objId)
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
        while (!reentrantLockMonitor.lock(lock, true)) {
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

    fun reentrantLockUnlock(lock: Any) {
        val t = Thread.currentThread().id
        reentrantLockMonitor.unlock(lock)
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
        scheduleNextOperation(true)
    }

    fun scheduleNextOperation(shouldBlockCurrentThread: Boolean) {
        // Our current design makes sure that reschedule is only called
        // by scheduled thread.
        assert(Thread.currentThread() is SFuzzThread || currentThreadId == Thread.currentThread().id)
        val currentThread = registeredThreads[currentThreadId]
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
        val context = registeredThreads[currentThreadId]!!

        if (enabledOperations.size > 1 || config!!.fullSchedule) {
            loggers.forEach {
                it.newOperationScheduled(context.pendingOperation!!,
                    Choice(index, context.index, enabledOperations.size))
            }
        }

        registeredThreads[currentThreadId]!!.pendingOperation = RunningOperation()
        if (currentThread != nextThread) {
            nextThread.unblock()
            if (shouldBlockCurrentThread) {
                currentThread?.block()
            }
        }
    }
}