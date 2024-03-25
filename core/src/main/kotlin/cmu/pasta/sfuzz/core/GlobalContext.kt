package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.concurrency.locks.LockManager
import cmu.pasta.sfuzz.core.concurrency.SFuzzThread
import cmu.pasta.sfuzz.core.concurrency.SynchronizationManager
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
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock

// TODO(aoli): make this a class maybe?
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
object GlobalContext {
    val registeredThreads = mutableMapOf<Long, ThreadContext>()
    var currentThreadId: Long = -1
    var mainThreadId: Long = -1
    var scheduler: Scheduler = FifoScheduler()
    var config: Configuration? = null
    private val lockManager = LockManager()
    private val volatileManager = VolatileManager()
    val syncManager = SynchronizationManager()
    val loggers = mutableListOf<LoggerBase>()
    var executor = Executors.newSingleThreadExecutor { r ->
        object : SFuzzThread() {
            override fun run() {
                r.run()
            }
        }
    }

    fun bootstrap() {
        executor = Executors.newSingleThreadExecutor { r ->
            object : SFuzzThread() {
                override fun run() {
                    r.run()
                }
            }
        }
    }

    fun mainExit() {
        val t = Thread.currentThread()
        val context = registeredThreads[t.id]!!
        context.state = ThreadState.Completed
        scheduleNextOperation(true)
    }

    fun start() {
        val t = Thread.currentThread();
        // We need to submit a dummy task to trigger the executor
        // thread creation
        executor.submit {}
        currentThreadId = t.id
        mainThreadId = t.id
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

        lockManager.done()
        assert(lockManager.waitingThreads.isEmpty())
        assert(syncManager.synchronizationPoints.isEmpty())
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
        syncManager.createWait(t, 1)
    }

    fun threadStartDone(t: Thread) {
        // Wait for the new thread runs.
        syncManager.wait(t)
    }

    fun monitorEnterDone(lock: Any) {
        syncManager.wait(lock)
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
        syncManager.signal(t)
        context.block()
    }

    fun threadUnpark(t: Thread) {
        val context = registeredThreads[t.id]!!
        if (context.state != ThreadState.Parked) {
            context.unparkSignaled = true
        } else {
            syncManager.createWait(t, 1)
            context.state = ThreadState.Enabled
            registeredThreads[t.id]?.pendingOperation = ThreadResumeOperation()
        }
    }

    fun threadUnparkDone(t: Thread) {
        if (registeredThreads[t.id]!!.state == ThreadState.Parked) {
            // SFuzz only needs to wait if `t` is parked and then
            // waken up by this `unpark` operation.
            syncManager.wait(t)
        }
    }

    fun threadRun() {
        var t = Thread.currentThread()
        registeredThreads[t.id]?.pendingOperation = ThreadStartOperation()
        registeredThreads[t.id]?.state = ThreadState.Enabled
        syncManager.signal(t)
        registeredThreads[t.id]?.block()
    }

    fun threadCompleted(t: Thread) {
        objectNotifyAll(t)
        registeredThreads[t.id]?.state = ThreadState.Completed
        // We do not want to send notify all because
        // we don't have monitor lock here.
        var size = 0
        lockManager.getLockContext(t).wakingThreads.let {
            for (thread in it) {
                registeredThreads[thread]!!.state = ThreadState.Enabled
            }
            size = it.size
        }
        syncManager.createWait(t, size)

        executor.submit {
            while (t.isAlive) {
                Thread.yield()
            }
            lockUnlockDone(t)
            scheduleNextOperation(false)
        }
    }

    private fun objectWaitImpl(waitingObject: Any, lockObject: Any) {
        val t = Thread.currentThread().id
        val objId = System.identityHashCode(waitingObject)
        val context = registeredThreads[t]!!
        context.pendingOperation = ObjectWaitOperation(objId)
        context.state = ThreadState.Enabled
        scheduleNextOperation(true)
        // If we resume executing, the Object.wait is executed. We should update the
        // state of current thread.


        context.blockedBy = waitingObject
        // No matter if an interrupt is signaled, we need to enter the `wait` method
        // first which will unlock the reentrant lock and tries to reacquire it.
        if (context.interruptSignaled) {
            lockManager.addWakingThread(lockObject, context.thread)
            context.pendingOperation = ThreadResumeOperation()
            context.state = ThreadState.Enabled
        } else {
            lockManager.addWaitingThread(waitingObject, Thread.currentThread())
            context.pendingOperation = PausedOperation()
            context.state = ThreadState.Paused
        }
        unlockImpl(lockObject, t, true, true, lockObject == waitingObject)

        // We need a daemon thread here because
        // `object.wait` release the monitor lock implicitly.
        // Therefore, we need to call `reentrantLockUnlockDone`
        // manually.
        executor.submit {
            while (registeredThreads[t]!!.thread.state == Thread.State.RUNNABLE) {
                Thread.yield()
            }
            lockUnlockDone(lockObject)
            scheduleNextOperation(false)
        }
    }

    fun objectWait(o: Any) {
        objectWaitImpl(o, o)
    }

    fun conditionAwait(o: Condition) {
        val lock = lockManager.lockFromCondition(o)
        objectWaitImpl(o, lock)
    }

    fun objectWaitDoneImpl(waitingObject: Any, lockObject: Any) {
        val t = Thread.currentThread()
        val context = registeredThreads[t.id]!!
        // We will unblock here only if the scheduler
        // decides to run it.
        while (context.state != ThreadState.Running) {
            syncManager.signal(lockObject)
            try {
                if (waitingObject is Condition) {
                    waitingObject.await()
                } else {
                    (waitingObject as Object).wait()
                }
            } catch (e: InterruptedException) {
                // We want to also catch interrupt exception here.
            }
        }
        // If a thread is enabled, the lock must be available.
        assert(lockManager.lock(lockObject, t.id, false, true))
        context.checkInterrupt()
    }

    fun threadInterrupt(t: Thread) {
        val context = registeredThreads[t.id]!!
        context.interruptSignaled = true
        if (context.blockedBy != null) {
            val lock = if (context.blockedBy is Condition) {
                lockManager.lockFromCondition(context.blockedBy as Condition)
            } else {
                context.blockedBy!!
            }
            lockManager.threadInterruptDuringObjectWait(context.blockedBy!!, lock, context)
        }
    }


    fun threadClearInterrupt(t: Thread): Boolean {
        val context = registeredThreads[t.id]!!
        val origin = context.interruptSignaled
        context.interruptSignaled = false
        return origin
    }

    fun objectWaitDone(o: Any) {
        objectWaitDoneImpl(o, o)
    }

    fun conditionAwaitDone(o: Condition) {
        objectWaitDoneImpl(o, lockManager.lockFromCondition(o))
    }

    fun objectNotifyImpl(waitingObject: Any, lockObject: Any) {
        val id = System.identityHashCode(waitingObject)
        lockManager.waitingThreads[id]?.let {
            if (it.size > 0) {
                val t = it.removeFirst()
                val context = registeredThreads[t]!!
                lockManager.addWakingThread(lockObject, context.thread)
                context.blockedBy = waitingObject
                it.remove(t)
                if (it.size == 0) {
                    lockManager.waitingThreads.remove(id)
                }
            }
        }

    }

    fun objectNotify(o: Any) {
        objectNotifyImpl(o, o)
    }

    fun conditionSignal(o: Condition) {
        objectNotifyImpl(o, lockManager.lockFromCondition(o))
    }

    fun objectNotifyAllImpl(waitingObject: Any, lockObject: Any) {
        val id = System.identityHashCode(waitingObject)
        lockManager.waitingThreads[id]?.let {
            if (it.size > 0) {
                for (t in it) {
                    val context = registeredThreads[t]!!
                    // We cannot enable the thread immediately because
                    // the thread is still waiting for the monitor lock.
                    context.blockedBy = waitingObject
                    lockManager.addWakingThread(lockObject, context.thread)
                }
                lockManager.waitingThreads.remove(id)
            }
        }
    }

    fun objectNotifyAll(o: Any) {
        objectNotifyAllImpl(o, o)
    }

    fun conditionSignalAll(o: Condition) {
        objectNotifyAllImpl(o, lockManager.lockFromCondition(o))
    }

    fun reentrantLockTrylock(lock: Any) {
        val t = Thread.currentThread().id
        val objId = System.identityHashCode(lock)
        registeredThreads[t]?.pendingOperation = ReentrantLockLockOperation(objId)
        registeredThreads[t]?.state = ThreadState.Enabled
        scheduleNextOperation(true)
        lockManager.lock(lock, t, false, false)
    }
    fun lockImpl(lock: Any, isMonitorLock: Boolean) {
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
        // TODO(aoli): we may need to store monitor locks and reentrant locks separately.
        // Consider the scenario where
        // ReentrantLock lock = new ReentrantLock();
        // lock.lock();
        // synchronized(lock) {
        //   lock.unlock();
        // }
        while (!lockManager.lock(lock, t, true, false)) {
            registeredThreads[t]?.state = ThreadState.Paused

            // We want to block current thread because we do
            // not want to rely on ReentrantLock. This allows
            // us to pick which Thread to run next if multiple
            // threads hold the same lock.
            scheduleNextOperation(true)
        }
    }

    fun monitorEnter(lock: Any) {
        lockImpl(lock, true)
    }

    fun lockLock(lock: Any) {
        lockImpl(lock, false)
    }

    fun reentrantReadWriteLockInit(readLock: ReadLock, writeLock: WriteLock) {
        lockManager.reentrantReadWriteLockInit(readLock, writeLock)
    }

    fun log(format: String, vararg args: Any) {
        val tid = Thread.currentThread().id
        val context = registeredThreads[tid]!!
        val data = "[${context.index}]: ${String.format(format, args)}"
        for (logger in loggers) {
            logger.applicationEvent(data)
        }
    }

    fun unlockImpl(lock: Any, tid: Long, sendNotifyAll: Boolean, unlockBecauseOfWait: Boolean, isMonitorLock: Boolean) {
        var waitingThreads = if (lockManager.unlock(lock, tid, unlockBecauseOfWait)) {
            lockManager.getNumThreadsBlockBy(lock)
        } else {
            0
        }
        // If this thread is unlocked because of wait.
        // We don't need to wait it to resume because
        // reentrant lock unlock is from that thread.
        if (unlockBecauseOfWait) {
            waitingThreads -= 1
        }
        if (waitingThreads > 0) {
            if (sendNotifyAll) {
                if (isMonitorLock) {
                    synchronized(lock) {
                        // Make some noise to wake up all waiting threads.
                        // This also ensure that the previous `notify` `notifyAll`
                        // are treated as no-ops.
                        (lock as Object).notifyAll()
                    }
                } else {
                    val reentrantLock = lock as ReentrantLock
                    reentrantLock.lock()
                    for (condition in lockManager.conditionFromLock(reentrantLock)) {
                        condition.signalAll()
                    }
                    reentrantLock.unlock()
                }
            }
            syncManager.createWait(lock, waitingThreads)
        }
    }

    fun lockUnlock(lock: Any) {
        unlockImpl(lock, Thread.currentThread().id, true, false, false)
    }

    fun monitorExit(lock: Any) {
        unlockImpl(lock, Thread.currentThread().id, true, false, true)
    }

    fun lockUnlockDone(lock: Any) {
        syncManager.wait(lock)
    }

    fun lockNewCondition(condition: Condition, lock: ReentrantLock) {
        lockManager.registerNewCondition(condition, lock)
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
        val currentThread = registeredThreads[currentThreadId]!!
        assert(Thread.currentThread() is SFuzzThread
                || currentThreadId == Thread.currentThread().id
                || currentThread.state == ThreadState.Enabled
                || currentThread.state == ThreadState.Completed)
        val enabledOperations = registeredThreads.values.toList()
            .filter { it.state == ThreadState.Enabled }
            .sortedBy { it.thread.id }

        if (enabledOperations.isEmpty()) {
            if (registeredThreads.all { it.value.state == ThreadState.Completed }) {
                // We are done here, we should go back to the main thread.
                if (currentThreadId != mainThreadId) {
                    registeredThreads[mainThreadId]!!.unblock()
                }
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
                it.newOperationScheduled(
                    nextThread.pendingOperation,
                    Choice(index, nextThread.index, enabledOperations.size))
            }
        }
        nextThread.state = ThreadState.Running
        if (currentThread != nextThread || currentThread.blockedBy != null) {
            unblockThread(nextThread)
        }
        if (currentThread != nextThread && shouldBlockCurrentThread) {
            currentThread.block()
        }
    }

    fun unblockThread(t: ThreadContext) {
        // If this object is blocked through JDK locks,
        // the thread is waiting for monitor locks.
        // We first need to give the thread lock
        // and then wakes it up through `notifyAll`.
        if (t.blockedBy != null) {
            // FIXME(aoli): relying on type check is not 100% correct,
            // because a thread can still be blocked by `condition.wait()`.
            if (t.blockedBy is Condition) {
                val condition = t.blockedBy as Condition
                val lock = lockManager.lockFromCondition(condition)
                lock.lock()
                condition.signalAll()
                lock.unlock()
            } else {
                synchronized(t.blockedBy!!) {
                    (t.blockedBy as Object).notifyAll()
                }
            }
            t.blockedBy = null
        } else {
            t.unblock()
        }
    }
}
