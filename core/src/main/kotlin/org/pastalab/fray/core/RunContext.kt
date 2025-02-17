package org.pastalab.fray.core

import java.io.PrintWriter
import java.io.StringWriter
import java.lang.Thread.UncaughtExceptionHandler
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.LockSupport
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock
import java.util.concurrent.locks.StampedLock
import kotlin.system.exitProcess
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.concurrency.HelperThread
import org.pastalab.fray.core.concurrency.ReentrantReadWriteLockCache
import org.pastalab.fray.core.concurrency.SynchronizationManager
import org.pastalab.fray.core.concurrency.operations.*
import org.pastalab.fray.core.concurrency.primitives.ConditionSignalContext
import org.pastalab.fray.core.concurrency.primitives.CountDownLatchContext
import org.pastalab.fray.core.concurrency.primitives.Interruptible
import org.pastalab.fray.core.concurrency.primitives.InterruptionType
import org.pastalab.fray.core.concurrency.primitives.LockContext
import org.pastalab.fray.core.concurrency.primitives.ObjectNotifyContext
import org.pastalab.fray.core.concurrency.primitives.ReadLockContext
import org.pastalab.fray.core.concurrency.primitives.ReentrantLockContext
import org.pastalab.fray.core.concurrency.primitives.ReferencedContextManager
import org.pastalab.fray.core.concurrency.primitives.SemaphoreContext
import org.pastalab.fray.core.concurrency.primitives.SignalContext
import org.pastalab.fray.core.concurrency.primitives.StampedLockContext
import org.pastalab.fray.core.concurrency.primitives.WriteLockContext
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.instrumentation.base.memory.VolatileManager
import org.pastalab.fray.rmi.ThreadState
import org.pastalab.fray.runtime.DeadlockException
import org.pastalab.fray.runtime.LivenessException
import org.pastalab.fray.runtime.Runtime.onReportError
import org.pastalab.fray.runtime.SyncurityCondition

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class RunContext(val config: Configuration) {
  val registeredThreads = mutableMapOf<Long, ThreadContext>()
  var currentThreadId: Long = -1
  var mainThreadId: Long = -1
  var bugFound: Throwable? = null
  var mainExiting = false
  var nanoTime = TimeUnit.SECONDS.toNanos(1577768400)
  val hashCodeMapper = ReferencedContextManager<Int>({ config.randomnessProvider.nextInt() })
  var forkJoinPool: ForkJoinPool? = null
  private val semaphoreManager = ReferencedContextManager {
    verifyOrReport(it is Semaphore) { "SemaphoreManager can only manage Semaphore objects" }
    SemaphoreContext(0)
  }
  private val volatileManager = VolatileManager(true)
  private val latchManager = ReferencedContextManager {
    verifyOrReport(it is CountDownLatch) { "CDL Manager only accepts CountDownLatch objects" }
    CountDownLatchContext(it as CountDownLatch, syncManager)
  }
  private val lockManager =
      ReferencedContextManager<LockContext> {
        when (it) {
          is ReentrantLock -> ReentrantLockContext()
          is ReadLock -> {
            val result =
                ReentrantReadWriteLockCache.getLock(it)?.let { lock ->
                  reentrantReadWriteLockInit(lock).first
                }
            if (result != null) {
              result
            } else {
              val context = ReadLockContext()
              context.writeLockContext = WriteLockContext()
              context.writeLockContext.readLockContext = context
              context
            }
          }
          is WriteLock -> {
            val result =
                ReentrantReadWriteLockCache.getLock(it)?.let { lock ->
                  reentrantReadWriteLockInit(lock).second
                }
            if (result != null) {
              result
            } else {
              val context = WriteLockContext()
              context.readLockContext = ReadLockContext()
              context.readLockContext.writeLockContext = context
              context
            }
          }
          else -> ReentrantLockContext()
        }
      }
  private val signalManager =
      ReferencedContextManager<SignalContext> {
        val lockContext = lockManager.getContext(it)
        val obj = ObjectNotifyContext(lockContext, it)
        lockContext.signalContexts.add(obj)
        obj
      }
  private val stampedLockManager =
      ReferencedContextManager<StampedLockContext> {
        verifyOrReport(it is StampedLock) {
          "StampedLockManager can only manage StampedLock objects"
        }
        StampedLockContext()
      }
  private var step = 0
  val syncManager = SynchronizationManager()
  var executor = HelperThread()

  init {
    executor.start()
  }

  fun reportError(e: Throwable) {
    if (e is LivenessException) {
      // Let's do not report liveness exceptions.
      return
    }
    if (bugFound == null && !config.executionInfo.ignoreUnhandledExceptions) {
      bugFound = e
      val sw = StringWriter()
      sw.append("Error: ${e}\n")
      if (e is org.pastalab.fray.runtime.DeadlockException) {
        for (registeredThread in registeredThreads.values) {
          if (registeredThread.state == ThreadState.Paused) {
            sw.append("Thread: ${registeredThread.thread}\n")
            sw.append("Stacktrace: \n")
            for (stackTraceElement in registeredThread.thread.stackTrace) {
              sw.append("\tat $stackTraceElement\n")
            }
          }
        }
      } else {
        sw.append("Thread: ${Thread.currentThread()}\n")
        e.printStackTrace(PrintWriter(sw))
      }

      if (e is FrayInternalError) {
        config.frayLogger.error(sw.toString())
        return
      }

      if (config.exploreMode && config.nextSavedIndex > 0) {
        config.nextSavedIndex++
        return
      }

      config.frayLogger.info(
          "Error found at iter: ${config.currentIteration}, step: $step, " +
              "Elapsed time: ${config.elapsedTime()}ms",
      )
      config.frayLogger.info(sw.toString())
      val recordingIndex = config.nextSavedIndex++
      config.saveToReportFolder(recordingIndex)
      config.frayLogger.info(
          "The recording is saved to ${config.report}/recording_$recordingIndex/")
      if (config.exploreMode || config.noExitWhenBugFound) {
        return
      }
      exitProcess(0)
    }
  }

  fun mainCleanup() {
    if (forkJoinPool != null) {
      forkJoinPool!!.shutdownNow()
      forkJoinPool!!.awaitTermination(1, TimeUnit.SECONDS)
      forkJoinPool = null
    }
  }

  fun mainExit() {
    val t = Thread.currentThread()
    val context = registeredThreads[t.id]!!
    mainExiting = true
    while (registeredThreads.any {
      it.value.state != ThreadState.Completed &&
          it.value.state != ThreadState.Created &&
          it.value != context
    }) {
      try {
        context.state = ThreadState.Enabled
        scheduleNextOperation(true)
      } catch (e: org.pastalab.fray.runtime.TargetTerminateException) {
        // If deadlock detected let's try to unblock one thread and continue.
        if (e is org.pastalab.fray.runtime.DeadlockException) {
          for (thread in registeredThreads.values) {
            if (thread.state == ThreadState.Paused) {
              val pendingOperation = thread.pendingOperation
              if (pendingOperation is Interruptible) {
                pendingOperation.unblockThread(thread.thread.id, InterruptionType.FORCE)
              }
            }
          }
        }
      }
    }
    context.state = ThreadState.Completed
    org.pastalab.fray.runtime.Runtime.DELEGATE = org.pastalab.fray.runtime.Delegate()
    done()
  }

  fun start() {
    val t = Thread.currentThread()
    config.scheduleObservers.forEach { it.onExecutionStart() }
    step = 0
    bugFound = null
    mainExiting = false
    currentThreadId = t.id
    mainThreadId = t.id
    registeredThreads[t.id] = ThreadContext(t, registeredThreads.size, this)
    registeredThreads[t.id]?.state = ThreadState.Enabled
    scheduleNextOperation(true)
  }

  fun done() {
    verifyOrReport(syncManager.synchronizationPoints.isEmpty())
    lockManager.done(false)
    signalManager.done()
    stampedLockManager.done()
    semaphoreManager.done()
    latchManager.done()

    registeredThreads.clear()
    config.scheduleObservers.forEach { it.onExecutionDone() }
    hashCodeMapper.done(false)
    nanoTime = TimeUnit.SECONDS.toNanos(1577768400)
  }

  fun shutDown() {
    org.pastalab.fray.runtime.Runtime.DELEGATE = org.pastalab.fray.runtime.Delegate()
    executor.stopHelperThread()
  }

  fun threadCreateDone(t: Thread) {
    val originalHanlder = t.uncaughtExceptionHandler
    val handler = UncaughtExceptionHandler { t, e ->
      onReportError(e)
      originalHanlder?.uncaughtException(t, e)
    }
    t.setUncaughtExceptionHandler(handler)
    registeredThreads[t.id] = ThreadContext(t, registeredThreads.size, this)
  }

  fun threadStart(t: Thread) {
    syncManager.createWait(t, 1)
  }

  fun threadStartDone(t: Thread) {
    // Wait for the new thread runs.
    syncManager.wait(t)
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

  fun threadParkDone(timed: Boolean) {
    val t = Thread.currentThread()
    val context = registeredThreads[t.id]!!

    if (!context.unparkSignaled && !context.interruptSignaled) {
      val supriousWakeup = config.randomnessProvider.nextInt() % 2 == 0
      if (supriousWakeup) {
        context.pendingOperation = ThreadResumeOperation(true)
        context.state = ThreadState.Enabled
      } else {
        context.pendingOperation = ParkBlocked(timed, context)
        context.state = ThreadState.Paused
        scheduleNextOperation(true)
      }
    } else if (context.unparkSignaled) {
      context.unparkSignaled = false
    }
  }

  fun threadUnpark(t: Thread) {
    val context = registeredThreads[t.id]
    if (context?.state == ThreadState.Paused && context?.pendingOperation is ParkBlocked) {
      context.state = ThreadState.Enabled
      context.pendingOperation = ThreadResumeOperation(true)
    } else if (context?.state == ThreadState.Enabled || context?.state == ThreadState.Running) {
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
      val context = registeredThreads[t.id]
      if (context?.state == ThreadState.Running || context?.state == ThreadState.Enabled) {
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
    //    lockManager.threadUnblockedDueToDeadlock(t)
    // We do not want to send notify all because
    // we don't have monitor lock here.
    var size = 0
    val lockContext = lockManager.getContext(t)
    lockContext.wakingThreads.let {
      for (thread in it) {
        thread.value.state = ThreadState.Enabled
      }
      size = it.size
    }
    syncManager.createWait(lockContext, size)
    executor.submit {
      while (t.isAlive) {
        Thread.yield()
      }
      context.isExiting = false
      lockUnlockDone(t)
      unlockImpl(lockContext, t.id, false, false, true)
      syncManager.synchronizationPoints.remove(System.identityHashCode(lockContext))
      scheduleNextOperationAndCheckDeadlock(false)
    }
  }

  private fun objectWaitImpl(waitingObject: Any, canInterrupt: Boolean, timed: Boolean) {
    val t = Thread.currentThread().id
    val objId = System.identityHashCode(waitingObject)
    val signalContext = signalManager.getContext(waitingObject)
    val lockContext = signalContext.lockContext
    val context = registeredThreads[t]!!
    context.pendingOperation = ObjectWaitOperation(objId)
    context.state = ThreadState.Enabled
    scheduleNextOperation(true)

    context.pendingOperation = ThreadResumeOperation(true)
    // First we need to check if current thread is interrupted.
    if (canInterrupt) {
      context.checkInterrupt()
    }

    // We also need to check if current thread holds the monitor lock.
    if (!lockContext.isLockHolder(t)) {
      // If current thread is not lock holder, we should just continue because
      // JVM will throw IllegalMonitorStateException.
      return
    }

    signalContext.addWaitingThread(context, timed, canInterrupt)

    unlockImpl(lockContext, t, true, true, signalContext is ObjectNotifyContext)

    val spuriousWakeup = config.randomnessProvider.nextInt() % 2 == 0
    // This is a spurious wakeup.
    // https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Object.html#wait(long,int)
    if (spuriousWakeup) {
      signalContext.unblockThread(t, InterruptionType.RESOURCE_AVAILABLE)
    }

    checkDeadlock {
      signalContext.unblockThread(t, InterruptionType.FORCE)
      verifyOrReport(lockContext.lock(context, false, true, false))
      syncManager.removeWait(signalContext.getSyncObject())
      context.pendingOperation = ThreadResumeOperation(true)
      context.state = ThreadState.Running
    }

    // We need a daemon thread here because
    // `object.wait` release the monitor lock implicitly.
    // Therefore, we need to call `reentrantLockUnlockDone`
    // manually.
    executor.submit {
      syncManager.wait(signalContext.getSyncObject())
      while (registeredThreads[t]!!.thread.state == Thread.State.RUNNABLE) {
        Thread.yield()
      }
      scheduleNextOperationAndCheckDeadlock(false)
    }
  }

  fun objectWait(o: Any, timed: Boolean) {
    objectWaitImpl(o, true, timed)
  }

  fun conditionAwait(o: Condition, canInterrupt: Boolean, timed: Boolean) {
    objectWaitImpl(o, canInterrupt, timed)
  }

  fun objectWaitDoneImpl(waitingObject: Any, canInterrupt: Boolean): Boolean {
    val t = Thread.currentThread()
    val context = registeredThreads[t.id]!!
    val signalContext = signalManager.getContext(waitingObject)
    // We will unblock here only if the scheduler
    // decides to run it.
    while (context.state != ThreadState.Running) {
      try {
        if (context.interruptSignaled) {
          Thread.interrupted()
        }
        syncManager.signal(signalContext.getSyncObject())
        if (waitingObject is Condition) {
          // TODO(aoli): Is this necessary?
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
    verifyOrReport(signalContext.lockContext.lock(context, false, true, false))
    val pendingOperation = context.pendingOperation
    verifyOrReport(pendingOperation is ThreadResumeOperation)
    if (canInterrupt) {
      context.checkInterrupt()
    }
    return (pendingOperation as ThreadResumeOperation).noTimeout
  }

  fun threadInterrupt(t: Thread) {
    val context = registeredThreads[t.id]!!
    context.interruptSignaled = true
    val pendingOperation = context.pendingOperation

    if (context.state == ThreadState.Running) {
      return
    }

    if (pendingOperation is Interruptible) {
      val result = pendingOperation.unblockThread(t.id, InterruptionType.INTERRUPT)
      if (result != null) {
        syncManager.createWait(result, 1)
        registeredThreads[Thread.currentThread().id]!!.pendingOperation =
            InterruptPendingOperation(result)
      }
    }
  }

  fun threadInterruptDone(t: Thread) {
    val context = registeredThreads[Thread.currentThread().id]!!
    val pendingOperation = context.pendingOperation
    if (pendingOperation is InterruptPendingOperation) {
      syncManager.wait(pendingOperation.waitingObject)
    }
    context.pendingOperation = ThreadResumeOperation(true)
  }

  fun threadClearInterrupt(t: Thread): Boolean {
    val context = registeredThreads[t.id]!!
    val origin = context.interruptSignaled
    context.interruptSignaled = false
    return origin
  }

  fun objectWaitDone(o: Any) {
    objectWaitDoneImpl(o, true)
  }

  fun conditionAwaitDone(o: Condition, canInterrupt: Boolean): Boolean {
    return objectWaitDoneImpl(o, canInterrupt)
  }

  fun objectNotifyImpl(waitingObject: Any) {
    val waitingContext = signalManager.getContext(waitingObject)
    waitingContext.signal(config.randomnessProvider, false)
  }

  fun objectNotify(o: Any) {
    objectNotifyImpl(o)
  }

  fun conditionSignal(o: Condition) {
    objectNotifyImpl(o)
  }

  fun objectNotifyAllImpl(waitingObject: Any) {
    val waitingContext = signalManager.getContext(waitingObject)
    waitingContext.signal(config.randomnessProvider, true)
  }

  fun objectNotifyAll(o: Any) {
    objectNotifyAllImpl(o)
  }

  fun conditionSignalAll(o: Condition) {
    objectNotifyAllImpl(o)
  }

  fun lockTryLock(lock: Any, canInterrupt: Boolean, timed: Boolean) {
    lockImpl(lock, false, false, canInterrupt, timed)
  }

  fun lockImpl(
      lock: Any,
      isMonitorLock: Boolean,
      shouldBlock: Boolean,
      canInterrupt: Boolean,
      timed: Boolean
  ) {
    val t = Thread.currentThread().id
    val context = registeredThreads[t]!!
    val lockContext = lockManager.getContext(lock)
    context.pendingOperation = LockLockOperation(lock)
    context.state = ThreadState.Enabled
    scheduleNextOperation(true)

    if (canInterrupt) {
      context.checkInterrupt()
    }

    val blockingWait = shouldBlock || timed

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
    while (!lockContext.lock(context, blockingWait, false, canInterrupt) && blockingWait) {
      context.state = ThreadState.Paused
      context.pendingOperation = LockBlocking(timed, lockContext)
      // We want to block current thread because we do
      // not want to rely on ReentrantLock. This allows
      // us to pick which Thread to run next if multiple
      // threads hold the same lock.
      scheduleNextOperation(true)
      if (canInterrupt) {
        context.checkInterrupt()
      }
      val pendingOperation = context.pendingOperation
      verifyOrReport(pendingOperation is ThreadResumeOperation)
      if (!(pendingOperation as ThreadResumeOperation).noTimeout && timed) {
        break
      }
    }
  }

  fun monitorEnter(lock: Any) {
    lockImpl(lock, true, true, false, false)
  }

  fun lockLock(lock: Any, canInterrupt: Boolean) {
    lockImpl(lock, false, true, canInterrupt, false)
  }

  fun reentrantReadWriteLockInit(
      lock: ReentrantReadWriteLock
  ): Pair<ReadLockContext, WriteLockContext> {
    val readLock = lock.readLock()
    val writeLock = lock.writeLock()
    val writeLockContext = WriteLockContext()
    val readLockContext = ReadLockContext()
    readLockContext.writeLockContext = writeLockContext
    writeLockContext.readLockContext = readLockContext
    lockManager.addContext(readLock, readLockContext)
    lockManager.addContext(writeLock, writeLockContext)
    ReentrantReadWriteLockCache.registerLock(lock)
    return Pair(readLockContext, writeLockContext)
  }

  fun unlockImpl(
      lockContext: LockContext,
      tid: Long,
      sendNotifyAll: Boolean,
      unlockBecauseOfWait: Boolean,
      isMonitorLock: Boolean
  ) {
    var waitingThreads =
        if (lockContext.unlock(tid, unlockBecauseOfWait, bugFound != null)) {
          lockContext.getNumThreadsWaitingForLockDueToSignal()
        } else {
          0
        }
    // If this thread is unlocked because of wait.
    // We don't need to wait it to resume because
    // reentrant lock unlock is from that thread.
    if (unlockBecauseOfWait) {
      waitingThreads -= 1
    }
    verifyOrReport(waitingThreads >= 0)
    if (waitingThreads > 0) {
      if (sendNotifyAll) {
        lockContext.signalContexts.forEach { it.sendSignalToObject() }
      }
      syncManager.createWait(lockContext, waitingThreads)
    }
  }

  fun lockUnlock(lock: Any) {
    unlockImpl(lockManager.getContext(lock), Thread.currentThread().id, true, false, false)
  }

  fun monitorExit(lock: Any) {
    unlockImpl(lockManager.getContext(lock), Thread.currentThread().id, true, false, true)
  }

  fun lockUnlockDone(lock: Any) {
    syncManager.wait(lockManager.getContext(lock))
  }

  fun lockNewCondition(condition: Condition, lock: Lock) {
    val lockContext = lockManager.getContext(lock)
    val conditionContext = ConditionSignalContext(lockContext, lock, condition)
    lockContext.signalContexts.add(conditionContext)
    signalManager.addContext(condition, conditionContext)
  }

  fun stampedLockLock(
      lock: StampedLock,
      shouldBlock: Boolean,
      canInterrupt: Boolean,
      timed: Boolean,
      isReadLock: Boolean
  ) {
    val t = Thread.currentThread().id
    val context = registeredThreads[t]!!
    context.pendingOperation = LockLockOperation(lock)
    context.state = ThreadState.Enabled
    scheduleNextOperation(true)

    val stampedLockContext = stampedLockManager.getContext(lock)
    val lockFun = if (isReadLock) stampedLockContext::readLock else stampedLockContext::writeLock

    while (!lockFun(context, shouldBlock, canInterrupt) && shouldBlock) {
      context.state = ThreadState.Paused
      context.pendingOperation = LockBlocking(timed, stampedLockContext)

      scheduleNextOperation(true)
      if (canInterrupt) {
        context.checkInterrupt()
      }
      val pendingOperation = context.pendingOperation
      if (!(pendingOperation as ThreadResumeOperation).noTimeout && timed) {
        break
      }
    }
  }

  fun stampedLockUnlock(lock: StampedLock, isReadLock: Boolean) {
    val stampedLockContext = stampedLockManager.getContext(lock)
    if (isReadLock) {
      stampedLockContext.unlockReadLock()
    } else {
      stampedLockContext.unlockWriteLock()
    }
  }

  fun stampedLockConvertToReadLock(lock: StampedLock, stamp: Long, newStamp: Long) {
    if (newStamp == 0L) {
      return
    }
    val stampedLockContext = stampedLockManager.getContext(lock)
    stampedLockContext.convertToReadLock(stamp)
  }

  fun stampedLockConvertToWriteLock(lock: StampedLock, stamp: Long, newStamp: Long) {
    if (newStamp == 0L) {
      return
    }
    val stampedLockContext = stampedLockManager.getContext(lock)
    stampedLockContext.convertToWriteLock(stamp)
  }

  fun stampedLockConvertToOptimisticReadLock(lock: StampedLock, stamp: Long, newStamp: Long) {
    val stampedLockContext = stampedLockManager.getContext(lock)
    stampedLockContext.convertToOptimisticReadLock(stamp)
  }

  fun semaphoreInit(sem: Semaphore) {
    val context = SemaphoreContext(sem.availablePermits())
    semaphoreManager.addContext(sem, context)
  }

  fun semaphoreAcquire(
      sem: Semaphore,
      permits: Int,
      shouldBlock: Boolean,
      canInterrupt: Boolean,
      timed: Boolean
  ) {
    val t = Thread.currentThread().id
    val context = registeredThreads[t]!!
    context.pendingOperation = LockLockOperation(sem)
    context.state = ThreadState.Enabled
    scheduleNextOperation(true)

    while (!semaphoreManager.getContext(sem).acquire(permits, shouldBlock, canInterrupt, context) &&
        shouldBlock) {
      context.pendingOperation = LockBlocking(timed, semaphoreManager.getContext(sem))
      context.state = ThreadState.Paused

      scheduleNextOperation(true)
      if (canInterrupt) {
        context.checkInterrupt()
      }
      val pendingOperation = context.pendingOperation
      if (!(pendingOperation as ThreadResumeOperation).noTimeout && timed) {
        break
      }
    }
  }

  fun semaphoreRelease(sem: Semaphore, permits: Int) {
    semaphoreManager.getContext(sem).release(permits)
  }

  fun semaphoreDrainPermits(sem: Semaphore): Int {
    return semaphoreManager.getContext(sem).drainPermits()
  }

  fun semaphoreReducePermits(sem: Semaphore, permits: Int) {
    semaphoreManager.getContext(sem).reducePermits(permits)
  }

  fun fieldOperation(
      obj: Any?,
      owner: String,
      name: String,
      type: org.pastalab.fray.runtime.MemoryOpType
  ) {
    if (!config.executionInfo.interleaveMemoryOps && !volatileManager.isVolatile(owner, name))
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

  fun atomicOperation(obj: Any, type: org.pastalab.fray.runtime.MemoryOpType) {
    val objId = System.identityHashCode(obj)
    memoryOperation(objId, type)
  }

  fun arrayOperation(obj: Any, index: Int, type: org.pastalab.fray.runtime.MemoryOpType) {
    if (!config.executionInfo.interleaveMemoryOps) return
    val objId = System.identityHashCode(obj)
    memoryOperation((31 * objId) + index, type)
  }

  fun unsafeOperation(obj: Any, offset: Long, type: org.pastalab.fray.runtime.MemoryOpType) {
    val objId = System.identityHashCode(obj)
    memoryOperation((31 * objId) + offset.toInt(), type)
  }

  fun memoryOperation(obj: Int, type: org.pastalab.fray.runtime.MemoryOpType) {
    val t = Thread.currentThread().id
    registeredThreads[t]?.pendingOperation = MemoryOperation(obj, type)
    registeredThreads[t]?.state = ThreadState.Enabled
    scheduleNextOperation(true)
  }

  fun latchAwait(latch: CountDownLatch, timed: Boolean) {
    val t = Thread.currentThread().id
    val objId = System.identityHashCode(latch)
    val context = registeredThreads[t]!!
    val latchContext = latchManager.getContext(latch)

    context.pendingOperation = ObjectWaitOperation(objId)
    context.state = ThreadState.Enabled
    scheduleNextOperation(true)

    if (latchContext.await(true, context)) {
      context.pendingOperation = CountDownLatchAwaitBlocking(timed, latchContext)
      context.state = ThreadState.Paused
      checkDeadlock {
        // We should not use [InterruptionType.FORCE] here because
        // The thread is not blocked by the latch yet.
        latchContext.unblockThread(t, InterruptionType.RESOURCE_AVAILABLE)
        context.state = ThreadState.Running
      }
      executor.submit {
        if (timed) {
          // this thread is blocked by Sync
          while (!context.sync.isBlocked()) {
            Thread.yield()
          }
        } else {
          // this thread is blocked by CDL
          while (context.thread.state == Thread.State.RUNNABLE) {
            Thread.yield()
          }
        }

        scheduleNextOperationAndCheckDeadlock(false)
      }
    } else {
      context.pendingOperation = ThreadResumeOperation(true)
    }
  }

  fun lockHasQueuedThreads(lock: Lock): Boolean {
    return lockManager.getContext(lock).hasQueuedThreads()
  }

  fun lockHasQueuedThread(lock: Lock, thread: Thread): Boolean {
    return lockManager.getContext(lock).hasQueuedThread(thread.id)
  }

  fun latchAwaitDone(latch: CountDownLatch): Boolean {
    val t = Thread.currentThread().id
    val context = registeredThreads[t]!!
    if (context.state != ThreadState.Running) {
      syncManager.signal(latch)
      context.block()
    }
    val pendingOperation = context.pendingOperation
    verifyOrReport(pendingOperation is ThreadResumeOperation)
    return (pendingOperation as ThreadResumeOperation).noTimeout
  }

  fun latchCountDown(latch: CountDownLatch) {
    val unblockedThreads = latchManager.getContext(latch).countDown()
    if (unblockedThreads > 0) {
      syncManager.createWait(latch, unblockedThreads)
    }
  }

  fun latchCountDownDone(latch: CountDownLatch) {
    syncManager.wait(latch)
  }

  fun threadSleepOperation() {
    val t = Thread.currentThread().id
    val context = registeredThreads[t]!!
    // Let's disable the delaying for the sleep operation for now.
    // We may want to make this configurable in the future.
    if (false) {
      context.checkInterrupt()
      context.pendingOperation = ThreadSleepBlocking(context)
      context.state = ThreadState.Paused
      scheduleNextOperation(true)
    } else {
      context.pendingOperation = ThreadResumeOperation(true)
      context.state = ThreadState.Enabled
      scheduleNextOperation(true)
    }
  }

  fun syncurityCondition(condition: SyncurityCondition) {
    val context = registeredThreads[Thread.currentThread().id]!!
    while (!condition.satisfied()) {
      context.pendingOperation = SyncurityWaitOperation(condition, context)
      context.state = ThreadState.Paused
      scheduleNextOperation(true)
    }
  }

  fun checkAndUnblockSyncurityOperations() {
    for (thread in registeredThreads.values) {
      if (thread.state == ThreadState.Paused && thread.pendingOperation is SyncurityWaitOperation) {
        val condition = (thread.pendingOperation as SyncurityWaitOperation).condition
        if (condition.satisfied()) {
          thread.pendingOperation = ThreadResumeOperation(true)
          thread.state = ThreadState.Enabled
        }
      }
    }
  }

  fun scheduleNextOperationAndCheckDeadlock(shouldBlockCurrentThread: Boolean) {
    try {
      scheduleNextOperation(shouldBlockCurrentThread)
    } catch (e: DeadlockException) {
      for (thread in registeredThreads.values) {
        if (thread.state == ThreadState.Paused) {
          val pendingOperation = thread.pendingOperation
          if (pendingOperation is Interruptible) {
            pendingOperation.unblockThread(thread.thread.id, InterruptionType.FORCE)
          }
        }
      }
      scheduleNextOperation(shouldBlockCurrentThread)
    }
  }

  fun checkDeadlock(cleanUp: () -> Unit) {
    val deadLock =
        if (registeredThreads.values.toList().none { it.schedulable() }) {
          unblockTimedOperations()
          registeredThreads.values.toList().none { it.schedulable() }
        } else {
          false
        }

    if (deadLock) {
      val e = DeadlockException()
      reportError(e)
      cleanUp()
      throw e
    }
  }

  fun yield() {
    registeredThreads[Thread.currentThread().id]!!.state = ThreadState.Enabled
    scheduleNextOperation(true)
  }

  fun unblockTimedOperations() {
    registeredThreads.values.forEach {
      val op = it.pendingOperation
      if (op is TimedBlockingOperation && op.timed) {
        op.unblockThread(it.thread.id, InterruptionType.TIMEOUT)
      }
    }
  }

  fun scheduleNextOperation(shouldBlockCurrentThread: Boolean) {
    // Our current design makes sure that reschedule is only called
    // by scheduled thread.
    val currentThread = registeredThreads[currentThreadId]!!
    verifyOrReport(
        Thread.currentThread() is HelperThread ||
            currentThreadId == Thread.currentThread().id ||
            currentThread.state == ThreadState.Enabled ||
            currentThread.state == ThreadState.Completed)
    verifyOrReport(registeredThreads.none { it.value.state == ThreadState.Running })

    if (bugFound != null &&
        !currentThread.isExiting &&
        currentThreadId != mainThreadId &&
        Thread.currentThread() !is HelperThread) {
      currentThread.state = ThreadState.Running
      // Let's try to break all running threads if a bug is found.
      throw RuntimeException()
    }

    checkAndUnblockSyncurityOperations()
    var enabledOperations =
        registeredThreads.values
            .toList()
            .filter { it.state == ThreadState.Enabled }
            .sortedBy { it.thread.id }
    if (mainExiting && (currentThreadId == mainThreadId || enabledOperations.size > 1)) {
      enabledOperations = enabledOperations.filter { it.thread.id != mainThreadId }
    }

    if (enabledOperations.isEmpty()) {
      unblockTimedOperations()
      enabledOperations =
          registeredThreads.values
              .toList()
              .filter { it.state == ThreadState.Enabled }
              .sortedBy { it.thread.id }
      if (mainExiting && (currentThreadId == mainThreadId || enabledOperations.size > 1)) {
        enabledOperations = enabledOperations.filter { it.thread.id != mainThreadId }
      }
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
        val e = org.pastalab.fray.runtime.DeadlockException()
        reportError(e)
        throw e
      }
    }

    step += 1
    if (config.executionInfo.maxScheduledStep in 1 ..< step &&
        !currentThread.isExiting &&
        Thread.currentThread() !is HelperThread &&
        !(mainExiting && currentThreadId == mainThreadId)) {
      currentThread.state = ThreadState.Running
      val e = LivenessException()
      reportError(e)
      throw e
    }

    val nextThread =
        if (enabledOperations.size == 1) {
          enabledOperations.first()
        } else {
          val thread = config.scheduler.scheduleNextOperation(enabledOperations, enabledOperations)
          config.scheduleObservers.forEach { it.onNewSchedule(enabledOperations, thread) }
          thread
        }

    currentThreadId = nextThread.thread.id
    nextThread.state = ThreadState.Running
    runThread(currentThread, nextThread)
    if (currentThread != nextThread && shouldBlockCurrentThread) {
      currentThread.block()
    }
  }

  fun runThread(currentThread: ThreadContext, nextThread: ThreadContext) {
    val pendingOperation = nextThread.pendingOperation
    when (pendingOperation) {
      is ConditionWakeBlocked -> {
        nextThread.pendingOperation = ThreadResumeOperation(pendingOperation.noTimeout)
        pendingOperation.conditionContext.sendSignalToObject()
        return
      }
      is ObjectWakeBlocked -> {
        nextThread.pendingOperation = ThreadResumeOperation(pendingOperation.noTimeout)
        pendingOperation.objectContext.sendSignalToObject()
        return
      }
    }
    if (currentThread != nextThread || Thread.currentThread() is HelperThread) {
      nextThread.unblock()
    }
  }

  fun hashCode(obj: Any): Int {
    val hashCodeMethod = obj.javaClass.getMethod("hashCode")
    return if (hashCodeMethod.declaringClass == Object::class.java) {
      hashCodeMapper.getContext(obj)
    } else {
      obj.hashCode()
    }
  }

  fun nanoTime(): Long {
    nanoTime += TimeUnit.MILLISECONDS.toNanos(10000)
    return nanoTime
  }

  fun currentTimeMillis(): Long {
    return nanoTime() / 1000000
  }

  fun instantNow(): Instant {
    return Instant.ofEpochMilli(currentTimeMillis())
  }

  fun getForkJoinPoolCommon(): ForkJoinPool {
    if (forkJoinPool == null) {
      forkJoinPool = ForkJoinPool()
    }
    return forkJoinPool!!
  }

  fun getThreadLocalRandomProbe(): Int {
    return registeredThreads[Thread.currentThread().id]!!.localRandomProbe
  }
}
