package cmu.pasta.fray.core

import cmu.pasta.fray.core.command.Configuration
import cmu.pasta.fray.core.concurrency.HelperThread
import cmu.pasta.fray.core.concurrency.SynchronizationManager
import cmu.pasta.fray.core.concurrency.locks.CountDownLatchManager
import cmu.pasta.fray.core.concurrency.locks.LockManager
import cmu.pasta.fray.core.concurrency.locks.SemaphoreManager
import cmu.pasta.fray.core.concurrency.operations.*
import cmu.pasta.fray.core.logger.LoggerBase
import cmu.pasta.fray.core.scheduler.Choice
import cmu.pasta.fray.core.scheduler.FifoScheduler
import cmu.pasta.fray.core.scheduler.Scheduler
import cmu.pasta.fray.instrumentation.memory.VolatileManager
import cmu.pasta.fray.runtime.DeadlockException
import cmu.pasta.fray.runtime.Delegate
import cmu.pasta.fray.runtime.LivenessException
import cmu.pasta.fray.runtime.MemoryOpType
import cmu.pasta.fray.runtime.Runtime
import cmu.pasta.fray.runtime.TargetTerminateException
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.LockSupport
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock
import kotlin.system.exitProcess

// TODO(aoli): make this a class maybe?
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
object GlobalContext {
  val registeredThreads = mutableMapOf<Long, ThreadContext>()
  var currentThreadId: Long = -1
  var mainThreadId: Long = -1
  var scheduler: Scheduler = FifoScheduler()
  var config: Configuration? = null
  var bugFound = false
  var mainExiting = false
  var nanoTime = System.nanoTime()
  val terminatingThread = mutableSetOf<Int>()
  private val lockManager = LockManager()
  private val semaphoreManager = SemaphoreManager()
  private val volatileManager = VolatileManager(true)
  private val latchManager = CountDownLatchManager()
  private var step = 0
  val syncManager = SynchronizationManager()
  val loggers = mutableListOf<LoggerBase>()
  var executor =
      Executors.newSingleThreadExecutor { r ->
        object : HelperThread() {
          override fun run() {
            r.run()
          }
        }
      }

  fun bootstrap() {
    executor =
        Executors.newSingleThreadExecutor { r ->
          object : HelperThread() {
            override fun run() {
              r.run()
            }
          }
        }
  }

  fun reportError(e: Throwable) {
    if (!bugFound && !config!!.executionInfo.ignoreUnhandledExceptions) {
      bugFound = true
      val sw = StringWriter()
      sw.append("Error found: ${e}\n")
      if (e is DeadlockException) {
        for (registeredThread in registeredThreads.values) {
          if (registeredThread.state == ThreadState.Paused) {
            sw.append("Thread: ${registeredThread.index}\n")
            sw.append("Stacktrace: \n")
            for (stackTraceElement in registeredThread.thread.stackTrace) {
              sw.append("\tat $stackTraceElement\n")
            }
          }
        }
      } else {
        e.printStackTrace(PrintWriter(sw))
      }
      for (logger in loggers) {
        logger.applicationEvent(sw.toString())
      }
      if (config!!.exploreMode || config!!.noExitWhenBugFound) {
        return
      }
      loggers.forEach { it.executionDone(bugFound) }
      exitProcess(-1)
    }
  }

  fun mainExit() {
    val t = Thread.currentThread()
    val context = registeredThreads[t.id]!!
    mainExiting = true
    while (registeredThreads.any {
      it.value.state != ThreadState.Completed && it.value != context
    }) {
      try {
        context.state = ThreadState.Enabled
        scheduleNextOperation(true)
      } catch (e: TargetTerminateException) {
        // If deadlock detected let's try to unblock one thread and continue.
        if (e is DeadlockException) {
          for (thread in registeredThreads.values) {
            if (thread.state == ThreadState.Paused) {
              thread.state = ThreadState.Enabled
              val pendingOperation = thread.pendingOperation
              thread.pendingOperation =
                  when (pendingOperation) {
                    is ObjectWaitBlocking -> {
                      ObjectWakeBlocking(pendingOperation.o)
                    }
                    is ConditionAwaitBlocking -> {
                      ConditionWakeBlocking(pendingOperation.condition)
                    }
                    is ObjectWakeBlocking -> {
                      pendingOperation
                    }
                    is ConditionWakeBlocking -> {
                      pendingOperation
                    }
                    else -> {
                      ThreadResumeOperation()
                    }
                  }
              lockManager.threadUnblockedDueToDeadlock(thread.thread)
              terminatingThread.add(thread.index)
              break
            }
          }
        }
      }
    }
    context.state = ThreadState.Completed
    Runtime.DELEGATE = Delegate()
    done()
  }

  fun start() {
    val t = Thread.currentThread()
    // We need to submit a dummy task to trigger the executor
    // thread creation
    executor.submit {}
    step = 0
    bugFound = false
    mainExiting = false
    currentThreadId = t.id
    mainThreadId = t.id
    registeredThreads[t.id] = ThreadContext(t, registeredThreads.size)
    registeredThreads[t.id]?.state = ThreadState.Enabled
    loggers.forEach { it.executionStart() }
    scheduleNextOperation(true)
  }

  fun done() {
    loggers.forEach { it.executionDone(bugFound) }

    assert(lockManager.waitingThreads.isEmpty())
    assert(syncManager.synchronizationPoints.isEmpty())
    lockManager.done()
    registeredThreads.clear()
    scheduler.done()
  }

  fun shutDown() {
    Runtime.DELEGATE = Delegate()
    executor.shutdown()

    for (logger in loggers) {
      logger.shutdown()
    }
  }

  fun registerLogger(l: LoggerBase) {
    loggers.add(l)
  }

  fun threadStart(t: Thread) {
    val originalHanlder = t.uncaughtExceptionHandler
    val handler = UncaughtExceptionHandler { t, e ->
      Runtime.onReportError(e)
      originalHanlder?.uncaughtException(t, e)
    }
    t.setUncaughtExceptionHandler(handler)
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
    val context = registeredThreads[t.id]!!

    context.state = ThreadState.Enabled
    context.pendingOperation = ParkBlocking()
    scheduleNextOperation(true)

    // Well, unpark is signaled everywhere. We cannot really rely on it to
    // block the thread.
    LockSupport.unpark(t)
  }

  fun threadParkDone() {
    val t = Thread.currentThread()
    val context = registeredThreads[t.id]!!

    if (!context.unparkSignaled && !context.interruptSignaled) {
      context.pendingOperation = ParkBlocking()
      context.state = ThreadState.Paused
      scheduleNextOperation(true)
    } else if (context.unparkSignaled) {
      context.unparkSignaled = false
    }
  }

  fun threadUnpark(t: Thread) {
    val context = registeredThreads[t.id]!!
    if (context.state == ThreadState.Paused && context.pendingOperation is ParkBlocking) {
      context.state = ThreadState.Enabled
      context.pendingOperation = ThreadResumeOperation()
    } else if (context.state == ThreadState.Enabled || context.state == ThreadState.Running) {
      context.unparkSignaled = true
    }
  }

  fun threadUnparkDone(t: Thread) {}

  fun threadRun() {
    var t = Thread.currentThread()
    registeredThreads[t.id]?.pendingOperation = ThreadStartOperation()
    registeredThreads[t.id]?.state = ThreadState.Enabled
    syncManager.signal(t)
    registeredThreads[t.id]?.block()
  }

  fun threadIsInterrupted(t: Thread, result: Boolean): Boolean {
    return result || registeredThreads[t.id]!!.interruptSignaled
  }

  fun threadGetState(t: Thread, state: Thread.State): Thread.State {
    if (state == Thread.State.WAITING ||
        state == Thread.State.TIMED_WAITING ||
        state == Thread.State.BLOCKED) {
      val context = registeredThreads[t.id]!!
      if (context.state == ThreadState.Running || context.state == ThreadState.Enabled) {
        return Thread.State.RUNNABLE
      }
    }
    return state
  }

  fun threadCompleted(t: Thread) {
    val context = registeredThreads[t.id]!!
    context.isExiting = true
    monitorEnter(t)
    objectNotifyAll(t)
    context.state = ThreadState.Completed
    lockManager.threadUnblockedDueToDeadlock(t)
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
      context.isExiting = false
      lockUnlockDone(t)
      unlockImpl(t, t.id, false, false, true)
      syncManager.synchronizationPoints.remove(System.identityHashCode(t))
      scheduleNextOperationAndCheckDeadlock(false)
    }
  }

  private fun objectWaitImpl(waitingObject: Any, lockObject: Any, canInterrupt: Boolean) {
    val t = Thread.currentThread().id
    val objId = System.identityHashCode(waitingObject)
    val context = registeredThreads[t]!!
    context.pendingOperation = ObjectWaitOperation(objId)
    context.state = ThreadState.Enabled
    scheduleNextOperation(true)
    // If we resume executing, the Object.wait is executed. We should update the
    // state of current thread.

    if (canInterrupt) {
      context.checkInterrupt()
    }
    // No matter if an interrupt is signaled, we need to enter the `wait` method
    // first which will unlock the reentrant lock and tries to reacquire it.
    if (lockObject == waitingObject) {
      context.pendingOperation = ObjectWaitBlocking(waitingObject)
    } else {
      context.pendingOperation = ConditionAwaitBlocking(waitingObject as Condition, canInterrupt)
    }
    context.state = ThreadState.Paused
    lockManager.addWaitingThread(waitingObject, Thread.currentThread())
    unlockImpl(lockObject, t, true, true, lockObject == waitingObject)
    checkDeadlock {
      context.pendingOperation = ThreadResumeOperation()
      assert(lockManager.lock(lockObject, t, false, true, false))
      syncManager.removeWait(lockObject)
      context.state = ThreadState.Running
    }

    // We need a daemon thread here because
    // `object.wait` release the monitor lock implicitly.
    // Therefore, we need to call `reentrantLockUnlockDone`
    // manually.
    executor.submit {
      while (registeredThreads[t]!!.thread.state == Thread.State.RUNNABLE) {
        Thread.yield()
      }
      lockUnlockDone(lockObject)
      scheduleNextOperationAndCheckDeadlock(false)
    }
  }

  fun objectWait(o: Any) {
    objectWaitImpl(o, o, true)
  }

  fun conditionAwait(o: Condition, canInterrupt: Boolean) {
    val lock = lockManager.lockFromCondition(o)
    objectWaitImpl(o, lock, canInterrupt)
  }

  fun objectWaitDoneImpl(waitingObject: Any, lockObject: Any, canInterrupt: Boolean) {
    val t = Thread.currentThread()
    val context = registeredThreads[t.id]!!
    // We will unblock here only if the scheduler
    // decides to run it.
    while (context.state != ThreadState.Running) {
      syncManager.signal(lockObject)
      try {
        if (context.interruptSignaled) {
          Thread.interrupted()
        }
        if (waitingObject is Condition) {
          if (canInterrupt) {
            waitingObject.await()
          } else {
            waitingObject.awaitUninterruptibly()
          }
        } else {
          (waitingObject as Object).wait()
        }
      } catch (e: InterruptedException) {
        // We want to also catch interrupt exception here.
      }
    }
    // If a thread is enabled, the lock must be available.
    assert(lockManager.lock(lockObject, t.id, false, true, false))
    if (canInterrupt) {
      context.checkInterrupt()
    }
  }

  fun threadInterrupt(t: Thread) {
    val context = registeredThreads[t.id]!!
    context.interruptSignaled = true

    if (context.state == ThreadState.Running || context.state == ThreadState.Enabled) {
      return
    }

    val pendingOperation = context.pendingOperation
    var waitingObject: Any? = null
    when (pendingOperation) {
      is ObjectWaitBlocking -> {
        if (lockManager.threadInterruptDuringObjectWait(
            pendingOperation.o, pendingOperation.o, context)) {
          syncManager.createWait(pendingOperation.o, 1)
          waitingObject = pendingOperation.o
        }
      }
      is ConditionAwaitBlocking -> {
        if (pendingOperation.canInterrupt) {
          val lock = lockManager.lockFromCondition(pendingOperation.condition)
          if (lockManager.threadInterruptDuringObjectWait(
              pendingOperation.condition, lock, context)) {
            syncManager.createWait(lock, 1)
            waitingObject = lock
          }
        }
      }
      is CountDownLatchAwaitBlocking -> {
        context.pendingOperation = ThreadResumeOperation()
        context.state = ThreadState.Enabled
        syncManager.createWait(pendingOperation.latch, 1)
        waitingObject = pendingOperation.latch
      }
      is ParkBlocking -> {
        context.pendingOperation = ThreadResumeOperation()
        context.state = ThreadState.Enabled
      }
      is LockBlocking -> {
        lockManager.getLockContext(pendingOperation.lock).interrupt(t.id)
      }
    }

    if (waitingObject != null) {
      registeredThreads[Thread.currentThread().id]!!.pendingOperation =
          InterruptPendingOperation(waitingObject)
    }
  }

  fun threadInterruptDone(t: Thread) {
    val context = registeredThreads[Thread.currentThread().id]!!
    val pendingOperation = context.pendingOperation
    if (pendingOperation is InterruptPendingOperation) {
      syncManager.wait(pendingOperation.waitingObject)
    }
    context.pendingOperation = ThreadResumeOperation()
  }

  fun threadClearInterrupt(t: Thread): Boolean {
    val context = registeredThreads[t.id]!!
    val origin = context.interruptSignaled
    context.interruptSignaled = false
    return origin
  }

  fun objectWaitDone(o: Any) {
    objectWaitDoneImpl(o, o, true)
  }

  fun conditionAwaitDone(o: Condition, canInterrupt: Boolean) {
    objectWaitDoneImpl(o, lockManager.lockFromCondition(o), canInterrupt)
  }

  fun objectNotifyImpl(waitingObject: Any, lockObject: Any) {
    val id = System.identityHashCode(waitingObject)
    lockManager.waitingThreads[id]?.let {
      if (it.size > 0) {
        val t = it.removeFirst()
        lockManager.threadWaitsFor.remove(t)
        val context = registeredThreads[t]!!
        lockManager.addWakingThread(lockObject, context.thread)
        if (waitingObject == lockObject) {
          context.pendingOperation = ObjectWakeBlocking(waitingObject)
        } else {
          context.pendingOperation = ConditionWakeBlocking(waitingObject as Condition)
        }
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
          lockManager.threadWaitsFor.remove(t)
          // We cannot enable the thread immediately because
          // the thread is still waiting for the monitor lock.
          if (waitingObject == lockObject) {
            context.pendingOperation = ObjectWakeBlocking(waitingObject)
          } else {
            context.pendingOperation = ConditionWakeBlocking(waitingObject as Condition)
          }
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

  fun lockTryLock(lock: Any, canInterrupt: Boolean) {
    lockImpl(lock, false, false, canInterrupt)
  }

  fun lockImpl(lock: Any, isMonitorLock: Boolean, shouldBlock: Boolean, canInterrupt: Boolean) {
    val t = Thread.currentThread().id
    val objId = System.identityHashCode(lock)
    val context = registeredThreads[t]!!
    context.pendingOperation = LockLockOperation(objId)
    context.state = ThreadState.Enabled
    scheduleNextOperation(true)

    if (canInterrupt) {
      context.checkInterrupt()
    }

    /**
     * We need a while loop here because even a thread unlock this thread and makes this thread
     * Enabled. It is still possible for a third thread to lock it again. t1 = {
     * 1. foo.lock();
     * 2. foo.unlock(); } t2 = {
     * 1. foo.lock();
     * 2. foo.unlock(); } t3 = {
     *     1. foo.lock();
     *     2. foo.unlock(); } t1.1, t2.1, t1.2, t3.1 will make t2.1 lock again.
     */
    // TODO(aoli): we may need to store monitor locks and reentrant locks separately.
    // Consider the scenario where
    // ReentrantLock lock = new ReentrantLock();
    // lock.lock();
    // synchronized(lock) {
    //   lock.unlock();
    // }
    while (!lockManager.lock(lock, t, shouldBlock, false, canInterrupt) && shouldBlock) {
      context.state = ThreadState.Paused
      context.pendingOperation = LockBlocking(lock)
      // We want to block current thread because we do
      // not want to rely on ReentrantLock. This allows
      // us to pick which Thread to run next if multiple
      // threads hold the same lock.
      scheduleNextOperation(true)
      if (canInterrupt) {
        context.checkInterrupt()
      }
    }
  }

  fun monitorEnter(lock: Any) {
    lockImpl(lock, true, true, false)
  }

  fun lockLock(lock: Any, canInterrupt: Boolean) {
    lockImpl(lock, false, true, canInterrupt)
  }

  fun reentrantReadWriteLockInit(readLock: ReadLock, writeLock: WriteLock) {
    lockManager.reentrantReadWriteLockInit(readLock, writeLock)
  }

  fun log(format: String, vararg args: Any) {
    val tid = Thread.currentThread().id
    val context = registeredThreads[tid]!!
    val data = "[${context.index}]: ${String.format(format, args)}\n"
    for (logger in loggers) {
      logger.applicationEvent(data)
    }
  }

  fun unlockImpl(
      lock: Any,
      tid: Long,
      sendNotifyAll: Boolean,
      unlockBecauseOfWait: Boolean,
      isMonitorLock: Boolean
  ) {
    var waitingThreads =
        if (lockManager.unlock(lock, tid, unlockBecauseOfWait)) {
          lockManager.getNumThreadsBlockBy(lock, isMonitorLock)
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

  fun lockNewCondition(condition: Condition, lock: Lock) {
    lockManager.registerNewCondition(condition, lock)
  }

  fun semaphoreInit(sem: Semaphore) {
    semaphoreManager.init(sem)
  }

  fun semaphoreAcquire(sem: Semaphore, permits: Int, shouldBlock: Boolean, canInterrupt: Boolean) {
    val t = Thread.currentThread().id
    val context = registeredThreads[t]!!
    val objId = System.identityHashCode(sem)
    context.pendingOperation = LockLockOperation(objId)
    context.state = ThreadState.Enabled
    scheduleNextOperation(true)

    while (!semaphoreManager.acquire(sem, permits, shouldBlock, canInterrupt) && shouldBlock) {
      context.state = ThreadState.Paused

      scheduleNextOperation(true)
      if (canInterrupt) {
        context.checkInterrupt()
      }
    }
  }

  fun semaphoreRelease(sem: Semaphore, permits: Int) {
    semaphoreManager.release(sem, permits)
  }

  fun semaphoreDrainPermits(sem: Semaphore): Int {
    return semaphoreManager.drainPermits(sem)
  }

  fun semaphoreReducePermits(sem: Semaphore, permits: Int) {
    semaphoreManager.reducePermits(sem, permits)
  }

  fun fieldOperation(obj: Any?, owner: String, name: String, type: MemoryOpType) {
    if (!config!!.executionInfo.interleaveMemoryOps && !volatileManager.isVolatile(owner, name))
        return
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

  fun arrayOperation(obj: Any, index: Int, type: MemoryOpType) {
    if (!config!!.executionInfo.interleaveMemoryOps) return
    val objId = System.identityHashCode(obj)
    memoryOperation((31 * objId) + index, type)
  }

  fun unsafeOperation(obj: Any, offset: Long, type: MemoryOpType) {
    val objId = System.identityHashCode(obj)
    memoryOperation((31 * objId) + offset.toInt(), type)
  }

  fun memoryOperation(obj: Int, type: MemoryOpType) {
    val t = Thread.currentThread().id
    registeredThreads[t]?.pendingOperation = MemoryOperation(obj, type)
    registeredThreads[t]?.state = ThreadState.Enabled
    scheduleNextOperation(true)
  }

  fun latchAwait(latch: CountDownLatch) {
    if (latchManager.await(latch, true)) {
      val t = Thread.currentThread().id
      val context = registeredThreads[t]!!
      context.pendingOperation = CountDownLatchAwaitBlocking(latch)
      context.state = ThreadState.Paused
      checkDeadlock {
        context.state = ThreadState.Running
        context.pendingOperation = ThreadResumeOperation()
      }
      executor.submit {
        while (registeredThreads[t]!!.thread.state == Thread.State.RUNNABLE) {
          Thread.yield()
        }
        scheduleNextOperationAndCheckDeadlock(false)
      }
    }
  }

  fun lockHasQueuedThreads(lock: Lock): Boolean {
    return lockManager.hasQueuedThreads(lock)
  }

  fun lockHasQueuedThread(lock: Lock, thread: Thread): Boolean {
    return lockManager.hasQueuedThread(lock, thread)
  }

  fun latchAwaitDone(latch: CountDownLatch) {
    val t = Thread.currentThread().id
    val context = registeredThreads[t]!!
    if (context.state != ThreadState.Running) {
      syncManager.signal(latch)
      context.block()
    }
  }

  fun latchCountDown(latch: CountDownLatch) {
    val unblockedThreads = latchManager.countDown(latch)
    if (unblockedThreads > 0) {
      syncManager.createWait(latch, unblockedThreads)
    }
  }

  fun latchCountDownDone(latch: CountDownLatch) {
    syncManager.wait(latch)
  }

  fun checkErrorAndExit() {
    if (bugFound) {
      val blockedThreads = mutableListOf<ThreadContext>()
      for (thread in registeredThreads.values) {
        if (thread.state != ThreadState.Running && thread.state != ThreadState.Completed) {
          thread.state = ThreadState.Running
          blockedThreads.add(thread)
        }
      }
      for (blockedThread in blockedThreads) {
        blockedThread.unblock()
      }
      throw LivenessException()
    }
  }

  fun scheduleNextOperationAndCheckDeadlock(shouldBlockCurrentThread: Boolean) {
    try {
      scheduleNextOperation(shouldBlockCurrentThread)
    } catch (e: TargetTerminateException) {
      for (thread in registeredThreads.values) {
        if (thread.state == ThreadState.Paused) {
          thread.state = ThreadState.Enabled
          lockManager.threadUnblockedDueToDeadlock(thread.thread)
          val pendingOperation = thread.pendingOperation
          when (pendingOperation) {
            is ObjectWaitBlocking -> {
              thread.pendingOperation = ObjectWakeBlocking(pendingOperation.o)
            }
            is ConditionAwaitBlocking -> {
              thread.pendingOperation = ConditionWakeBlocking(pendingOperation.condition)
            }
            is CountDownLatchAwaitBlocking -> {
              val releasedThreads = latchManager.release(pendingOperation.latch)
              syncManager.createWait(pendingOperation.latch, releasedThreads)
              while (pendingOperation.latch.count > 0) {
                pendingOperation.latch.countDown()
              }
              syncManager.wait(pendingOperation.latch)
            }
          }
          scheduleNextOperation(shouldBlockCurrentThread)
          break
        }
      }
    }
  }

  fun checkDeadlock(cleanUp: () -> Unit) {
    val deadLock = registeredThreads.values.toList().none { it.schedulable() }
    if (deadLock) {
      val e = DeadlockException()
      reportError(e)
      registeredThreads[Thread.currentThread().id]!!.state = ThreadState.Enabled
      lockManager.threadUnblockedDueToDeadlock(Thread.currentThread())
      cleanUp()
      throw e
    }
  }

  fun yield() {
    registeredThreads[Thread.currentThread().id]!!.state = ThreadState.Enabled
    scheduleNextOperation(true)
  }

  fun scheduleNextOperation(shouldBlockCurrentThread: Boolean) {
    // Our current design makes sure that reschedule is only called
    // by scheduled thread.
    val currentThread = registeredThreads[currentThreadId]!!
    assert(
        Thread.currentThread() is HelperThread ||
            currentThreadId == Thread.currentThread().id ||
            currentThread.state == ThreadState.Enabled ||
            currentThread.state == ThreadState.Completed)
    assert(registeredThreads.none { it.value.state == ThreadState.Running })

    if (bugFound &&
        !currentThread.isExiting &&
        currentThreadId != mainThreadId &&
        !(Thread.currentThread() is HelperThread)) {
      currentThread.state = ThreadState.Running
      throw RuntimeException()
    }

    var enabledOperations =
        registeredThreads.values
            .toList()
            .filter { it.state == ThreadState.Enabled }
            .sortedBy { it.thread.id }
    if (mainExiting && (currentThreadId == mainThreadId || enabledOperations.size > 1)) {
      enabledOperations = enabledOperations.filter { it.thread.id != mainThreadId }
    }

    if (enabledOperations.isEmpty()) {
      if (registeredThreads.all { it.value.state == ThreadState.Completed }) {
        // We are done here, we should go back to the main thread.
        if (currentThreadId != mainThreadId) {
          registeredThreads[mainThreadId]!!.unblock()
        }
        return
      } else if (!currentThread.isExiting || Thread.currentThread() is HelperThread) {
        // Deadlock detected
        val e = DeadlockException()
        reportError(e)
        throw e
      }
    }

    step += 1
    if (config!!.executionInfo.maxScheduledStep in 1 ..< step &&
        !currentThread.isExiting &&
        Thread.currentThread() !is HelperThread &&
        !(mainExiting && currentThreadId == mainThreadId)) {
      currentThread.state = ThreadState.Running
      val e = LivenessException()
      reportError(e)
      throw e
    }

    val nextThread = scheduler.scheduleNextOperation(enabledOperations)
    val index = enabledOperations.indexOf(nextThread)
    currentThreadId = nextThread.thread.id

    if (enabledOperations.size > 1 || config!!.fullSchedule) {
      loggers.forEach {
        it.newOperationScheduled(
            nextThread.pendingOperation,
            Choice(
                index,
                nextThread.index,
                enabledOperations.size,
                enabledOperations.map { it.index },
                nextThread.pendingOperation.toString()))
      }
    }
    nextThread.state = ThreadState.Running
    unblockThread(currentThread, nextThread)
    if (currentThread != nextThread && shouldBlockCurrentThread) {
      currentThread.block()
    }
  }

  fun unblockThread(currentThread: ThreadContext, nextThread: ThreadContext) {
    val pendingOperation = nextThread.pendingOperation
    when (pendingOperation) {
      is ConditionWakeBlocking -> {
        nextThread.pendingOperation = ThreadResumeOperation()
        val lock = lockManager.lockFromCondition(pendingOperation.condition)
        lock.lock()
        pendingOperation.condition.signalAll()
        lock.unlock()
        return
      }
      is ObjectWakeBlocking -> {
        nextThread.pendingOperation = ThreadResumeOperation()
        synchronized(pendingOperation.o) { (pendingOperation.o as Object).notifyAll() }
        return
      }
    }
    if (currentThread != nextThread) {
      nextThread.unblock()
    }
  }

  fun nanoTime(): Long {
    nanoTime += 1000L
    return nanoTime
  }
}
