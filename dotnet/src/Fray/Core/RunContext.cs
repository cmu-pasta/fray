using System.Runtime.CompilerServices;
using System.Text;
using Fray.Core.Contexts;
using Fray.Core.Observers;
using Fray.Core.Operations;
using Fray.Core.Randomness;
using Fray.Core.Scheduling;

namespace Fray.Core;

/// <summary>
/// Central state of one controlled execution: registered threads, resource
/// models, and the scheduling loop. Exactly one controlled thread runs at any
/// time; every controlled primitive funnels through
/// <see cref="ScheduleNextOperation"/>, which consults the scheduler and hands
/// execution over to the chosen thread.
///
/// This is a C# port of <c>org.pastalab.fray.core.RunContext</c>. The major
/// difference: the JVM implementation instruments real concurrency primitives
/// and therefore has to coordinate with them (helper thread, synchronization
/// points). This port models primitives entirely inside the engine — a
/// controlled thread only ever parks on its own <see cref="ThreadContext"/>
/// signal — which removes that machinery.
/// </summary>
public sealed class RunContext
{
    private readonly List<ThreadContext> _registeredThreads = new();
    private readonly List<ThreadContext> _enabledBuffer = new();

    private readonly ConditionalWeakTable<object, LockContext> _lockManager = new();
    private readonly ConditionalWeakTable<object, SignalContext> _signalManager = new();
    private readonly ConditionalWeakTable<object, SemaphoreContext> _semaphoreManager = new();
    private readonly ConditionalWeakTable<object, CountDownLatchContext> _latchManager = new();
    private readonly ConditionalWeakTable<object, Tuple<ReadLockContext, WriteLockContext>> _rwLockManager = new();

    private int _currentThreadIndex = -1;
    private int _mainThreadIndex = -1;
    private bool _mainExiting;

    public FrayConfiguration Config { get; }
    public IScheduler Scheduler { get; }
    public IRandomness Randomness { get; }
    public IReadOnlyList<IScheduleObserver> Observers { get; }

    public Exception? BugFound { get; private set; }
    public string? ErrorReport { get; private set; }
    public ControlledRandom? RandomSnapshotAtError { get; private set; }
    public int Step { get; private set; }

    public IReadOnlyList<ThreadContext> RegisteredThreads => _registeredThreads;

    /// <summary>True once the execution is only draining remaining threads.</summary>
    internal bool IsWindingDown =>
        BugFound != null || (_mainExiting && Config.AbortThreadsAfterMainExit);

    public RunContext(FrayConfiguration config, IScheduler scheduler, IRandomness randomness, IReadOnlyList<IScheduleObserver> observers)
    {
        Config = config;
        Scheduler = scheduler;
        Randomness = randomness;
        Observers = observers;
    }

    private static ThreadContext CurrentThread() =>
        FrayRuntime.CurrentThreadContext
        ?? throw new FrayInternalException("Controlled operation invoked from an unregistered thread.");

    internal void VerifyOrReport(bool condition, string message)
    {
        if (!condition)
        {
            ReportError(new FrayInternalException(message));
        }
    }

    // ---------------------------------------------------------------------
    // Error reporting
    // ---------------------------------------------------------------------

    public void ReportError(Exception e)
    {
        // Liveness violations only abort the iteration; they are not bugs.
        if (e is LivenessException)
        {
            return;
        }
        // A deadlock between leftover threads while the main thread exits is
        // expected when those threads are about to be aborted.
        if (e is DeadlockException && _mainExiting && Config.AbortThreadsAfterMainExit)
        {
            return;
        }
        if (BugFound != null)
        {
            return;
        }
        BugFound = e;
        RandomSnapshotAtError = (Randomness as ControlledRandom)?.Snapshot();

        var report = new StringBuilder();
        report.Append($"Error: {e.GetType().Name}: {e.Message}\n");
        report.Append($"Found at step {Step}.\n");
        if (e is DeadlockException)
        {
            foreach (var thread in _registeredThreads)
            {
                if (thread.State == FrayThreadState.Blocked)
                {
                    var resource = (thread.PendingOperation as BlockedOperation)?.ResourceInfo;
                    report.Append($"Blocked thread: {thread.Name} (index {thread.Index}), operation {thread.PendingOperation}, resource {resource}\n");
                }
            }
        }
        else
        {
            report.Append($"Thread: {FrayRuntime.CurrentThreadContext?.Name ?? Thread.CurrentThread.Name ?? "<unknown>"}\n");
            report.Append(e.ToString());
            report.Append('\n');
        }
        ErrorReport = report.ToString();

        foreach (var observer in Observers)
        {
            observer.OnReportError(e);
        }
    }

    // ---------------------------------------------------------------------
    // Execution lifecycle
    // ---------------------------------------------------------------------

    /// <summary>Registers the calling thread as the main thread and starts the execution.</summary>
    public void Start()
    {
        Step = 0;
        BugFound = null;
        var context = RegisterThread(Thread.CurrentThread, "fray-main", parentIndex: -1);
        _mainThreadIndex = context.Index;
        _currentThreadIndex = context.Index;
        FrayRuntime.CurrentThreadContext = context;
        context.State = FrayThreadState.Runnable;
        foreach (var observer in Observers)
        {
            observer.OnExecutionStart();
        }
        ScheduleNextOperation(true);
    }

    /// <summary>
    /// Called by the main thread after the test body returned: keeps scheduling
    /// the remaining threads until they all finished (or get aborted).
    /// </summary>
    public void WaitForAllThreadsToFinish()
    {
        var context = CurrentThread();
        while (_registeredThreads.Any(t =>
                   t.State != FrayThreadState.Completed &&
                   t.State != FrayThreadState.Created &&
                   t != context))
        {
            try
            {
                context.State = FrayThreadState.MainExiting;
                _mainExiting = true;
                ScheduleNextOperation(true);
            }
            catch (TargetTerminateException e)
            {
                // If a deadlock is detected, force-unblock the blocked threads;
                // they will observe the abort at their next schedule point.
                if (e is DeadlockException)
                {
                    ForceUnblockAllBlockedThreads();
                }
            }
        }
    }

    public void MainExit()
    {
        var context = CurrentThread();
        context.State = FrayThreadState.Completed;
        Done();
    }

    private void Done()
    {
        foreach (var observer in Observers)
        {
            observer.OnExecutionDone(BugFound);
        }
        _mainExiting = false;
    }

    // ---------------------------------------------------------------------
    // Thread lifecycle
    // ---------------------------------------------------------------------

    private ThreadContext RegisterThread(Thread thread, string name, int parentIndex)
    {
        var context = new ThreadContext(this, thread, name, _registeredThreads.Count, parentIndex);
        _registeredThreads.Add(context);
        return context;
    }

    /// <summary>Registers a child thread created by the currently running thread.</summary>
    public ThreadContext ThreadCreate(Thread thread, string name)
    {
        var parent = CurrentThread();
        return RegisterThread(thread, name, parent.Index);
    }

    /// <summary>
    /// Entry point of a controlled child thread: registers itself on its own OS
    /// thread, signals the parent, and parks until the scheduler picks it.
    /// </summary>
    public void ThreadRun(ThreadContext context, ManualResetEventSlim started)
    {
        FrayRuntime.CurrentThreadContext = context;
        context.State = FrayThreadState.Runnable;
        started.Set();
        context.Block();
    }

    /// <summary>
    /// Completion protocol of a controlled thread: wake joiners through the
    /// thread handle's monitor, mark the thread completed, and hand execution
    /// to the next thread without parking (the OS thread is about to die).
    /// </summary>
    public void ThreadCompleted(object threadHandle, ThreadContext context)
    {
        context.IsExiting = true;
        try
        {
            MonitorEnter(threadHandle, shouldRetry: true);
            ObjectPulseImpl(threadHandle, all: true, checkHolder: false);
            MonitorExit(threadHandle);
        }
        catch (TargetTerminateException)
        {
            // The execution is winding down; joiners get force-unblocked elsewhere.
        }
        context.State = FrayThreadState.Completed;
        try
        {
            ScheduleNextOperationAndCheckDeadlock(false);
        }
        catch (TargetTerminateException)
        {
        }
    }

    public void ThreadInterrupt(ThreadContext target)
    {
        target.InterruptSignaled = true;
        if (target.State == FrayThreadState.Running)
        {
            return;
        }
        if (target.PendingOperation is BlockedOperation blocked)
        {
            blocked.Unblock(target, InterruptionType.Interrupt);
        }
    }

    public void Yield()
    {
        CurrentThread().State = FrayThreadState.Runnable;
        ScheduleNextOperation(true);
    }

    public void ThreadSleep(long milliseconds)
    {
        var context = CurrentThread();
        context.CheckInterrupt();
        if (Config.SleepAsYield)
        {
            context.State = FrayThreadState.Runnable;
            ScheduleNextOperation(true);
        }
        else
        {
            context.PendingOperation = new SleepBlocked(context, Environment.TickCount64 + milliseconds);
            context.State = FrayThreadState.Blocked;
            ScheduleNextOperation(true);
        }
        context.CheckInterrupt();
    }

    // ---------------------------------------------------------------------
    // Monitor / lock operations
    // ---------------------------------------------------------------------

    private LockContext GetLockContext(object lockObject) =>
        _lockManager.GetValue(lockObject, o => new ReentrantLockContext(o));

    private ObjectNotifyContext GetObjectNotifyContext(object obj) =>
        (ObjectNotifyContext)_signalManager.GetValue(obj, o =>
        {
            var lockContext = GetLockContext(o);
            var signalContext = new ObjectNotifyContext(lockContext);
            lockContext.SignalContexts.Add(signalContext);
            return signalContext;
        });

    public void MonitorEnter(object lockObject, bool shouldRetry = false) =>
        LockImpl(GetLockContext(lockObject), lockObject, shouldBlock: true, canInterrupt: false,
            blockedUntil: BlockedOperation.NotTimed, shouldRetry: shouldRetry);

    public void MonitorExit(object lockObject)
    {
        var context = CurrentThread();
        var lockContext = GetLockContext(lockObject);
        if (!lockContext.IsLockHolder(context))
        {
            throw new SynchronizationLockException("Monitor is not held by the current thread.");
        }
        UnlockImpl(lockContext, context, unlockBecauseOfWait: false);
    }

    public bool ObjectWait(object lockObject, long blockedUntil, bool canInterrupt) =>
        WaitImpl(GetObjectNotifyContext(lockObject), ObjectIds.Of(lockObject), blockedUntil, canInterrupt);

    public void ObjectPulse(object lockObject, bool all) => ObjectPulseImpl(lockObject, all, checkHolder: true);

    private void ObjectPulseImpl(object lockObject, bool all, bool checkHolder)
    {
        var context = CurrentThread();
        var lockContext = GetLockContext(lockObject);
        if (checkHolder && !lockContext.IsLockHolder(context))
        {
            throw new SynchronizationLockException("Monitor is not held by the current thread.");
        }
        if (_signalManager.TryGetValue(lockObject, out var signalContext))
        {
            signalContext.Signal(Randomness, all);
        }
    }

    public void LockLock(object lockObject, bool canInterrupt) =>
        LockImpl(GetLockContext(lockObject), lockObject, shouldBlock: true, canInterrupt: canInterrupt,
            blockedUntil: BlockedOperation.NotTimed, shouldRetry: false);

    public bool LockTryLock(object lockObject, bool canInterrupt, long blockedUntil)
    {
        var lockContext = GetLockContext(lockObject);
        LockImpl(lockContext, lockObject, shouldBlock: false, canInterrupt: canInterrupt,
            blockedUntil: blockedUntil, shouldRetry: false);
        return lockContext.IsLockHolder(CurrentThread());
    }

    public void LockUnlock(object lockObject)
    {
        var context = CurrentThread();
        var lockContext = GetLockContext(lockObject);
        if (!lockContext.IsLockHolder(context))
        {
            throw new SynchronizationLockException("Lock is not held by the current thread.");
        }
        UnlockImpl(lockContext, context, unlockBecauseOfWait: false);
    }

    public void RegisterCondition(object condition, object lockObject)
    {
        var lockContext = GetLockContext(lockObject);
        var conditionContext = new ConditionSignalContext(lockContext, condition);
        lockContext.SignalContexts.Add(conditionContext);
        _signalManager.AddOrUpdate(condition, conditionContext);
    }

    private ConditionSignalContext GetConditionContext(object condition, object lockObject)
    {
        if (!_signalManager.TryGetValue(condition, out var signalContext))
        {
            RegisterCondition(condition, lockObject);
            _signalManager.TryGetValue(condition, out signalContext);
        }
        return (ConditionSignalContext)signalContext!;
    }

    public bool ConditionAwait(object condition, object lockObject, long blockedUntil, bool canInterrupt) =>
        WaitImpl(GetConditionContext(condition, lockObject), ObjectIds.Of(condition), blockedUntil, canInterrupt);

    public void ConditionSignal(object condition, object lockObject, bool all)
    {
        var context = CurrentThread();
        var signalContext = GetConditionContext(condition, lockObject);
        if (!signalContext.LockContext.IsLockHolder(context))
        {
            throw new SynchronizationLockException("Lock is not held by the current thread.");
        }
        signalContext.Signal(Randomness, all);
    }

    /// <summary>
    /// The wait protocol shared by monitor waits and condition awaits: a
    /// schedule point, releasing the lock, parking until signaled *and*
    /// scheduled, then reacquiring the lock.
    /// </summary>
    private bool WaitImpl(SignalContext signalContext, int resourceId, long blockedUntil, bool canInterrupt)
    {
        var context = CurrentThread();
        var lockContext = signalContext.LockContext;

        context.PendingOperation = new ObjectWaitOperation(resourceId);
        context.State = FrayThreadState.Runnable;
        ScheduleNextOperation(true);

        if (canInterrupt)
        {
            context.CheckInterrupt();
        }
        if (!lockContext.IsLockHolder(context))
        {
            throw new SynchronizationLockException("Wait requires the lock to be held by the current thread.");
        }

        signalContext.AddWaitingThread(context, blockedUntil, canInterrupt);
        UnlockImpl(lockContext, context, unlockBecauseOfWait: true);

        // Spurious wakeups are allowed by both JVM and .NET monitor semantics;
        // exploring them finds bugs in code that does not re-check conditions.
        if (Config.AllowSpuriousWakeups && Randomness.NextInt() % 2 == 0)
        {
            signalContext.UnblockThread(context, InterruptionType.ResourceAvailable);
        }

        CheckDeadlock(() =>
        {
            signalContext.UnblockThread(context, InterruptionType.Force);
            var locked = lockContext.Lock(context, shouldBlock: false, lockBecauseOfWait: true, canInterrupt: false);
            VerifyOrReport(locked, "Lock must be available after forced wakeup.");
            context.PendingOperation = new ThreadResumeOperation(true);
            context.State = FrayThreadState.Running;
        });

        // Parks until a signal (or timeout/interrupt) made this thread runnable
        // and the scheduler picked it again.
        ScheduleNextOperation(true);

        var pendingOperation = context.PendingOperation;
        VerifyOrReport(pendingOperation is ThreadResumeOperation, "Wait resumed with unexpected operation.");
        // By invariant the lock is reacquirable when a waiting thread is
        // rescheduled; the loop only spins during forced wind-down recovery.
        while (!lockContext.Lock(context, shouldBlock: true, lockBecauseOfWait: true, canInterrupt: false))
        {
            context.State = FrayThreadState.Blocked;
            context.PendingOperation = new LockBlocked(BlockedOperation.NotTimed, lockContext);
            ScheduleNextOperationAndCheckDeadlock(true);
        }
        if (canInterrupt)
        {
            context.CheckInterrupt();
        }
        return (pendingOperation as ThreadResumeOperation)?.NoTimeout ?? true;
    }

    private void LockImpl(LockContext lockContext, object lockObject, bool shouldBlock, bool canInterrupt,
        long blockedUntil, bool shouldRetry)
    {
        var context = CurrentThread();
        context.PendingOperation = new LockLockOperation(lockObject);
        context.State = FrayThreadState.Runnable;
        ScheduleNextOperation(true);

        if (canInterrupt)
        {
            context.CheckInterrupt();
        }

        var blockingWait = shouldBlock || blockedUntil != BlockedOperation.NotTimed;
        // A loop is required: even when an unlock made this thread runnable, a
        // third thread may have acquired the lock before this thread ran.
        while (!lockContext.Lock(context, blockingWait, false, canInterrupt) && blockingWait)
        {
            context.State = FrayThreadState.Blocked;
            context.PendingOperation = new LockBlocked(blockedUntil, (Acquirable)lockContext);
            if (shouldRetry)
            {
                ScheduleNextOperationAndCheckDeadlock(true);
            }
            else
            {
                ScheduleNextOperation(true);
            }
            if (canInterrupt)
            {
                context.CheckInterrupt();
            }
            var pendingOperation = context.PendingOperation;
            VerifyOrReport(pendingOperation is ThreadResumeOperation, "Lock acquisition resumed with unexpected operation.");
            if (pendingOperation is ThreadResumeOperation { NoTimeout: false } &&
                blockedUntil != BlockedOperation.NotTimed)
            {
                break;
            }
        }
    }

    private void UnlockImpl(LockContext lockContext, ThreadContext context, bool unlockBecauseOfWait) =>
        lockContext.Unlock(context, unlockBecauseOfWait, BugFound != null);

    // ---------------------------------------------------------------------
    // Reader-writer locks
    // ---------------------------------------------------------------------

    private (ReadLockContext Read, WriteLockContext Write) GetRwLockContexts(object rwLock)
    {
        var pair = _rwLockManager.GetValue(rwLock, o =>
        {
            var readContext = new ReadLockContext(o);
            var writeContext = new WriteLockContext(o);
            readContext.WriteLockContext = writeContext;
            writeContext.ReadLockContext = readContext;
            return Tuple.Create(readContext, writeContext);
        });
        return (pair.Item1, pair.Item2);
    }

    public void RwLockLock(object rwLock, bool isWrite) =>
        LockImpl(GetRwLockContext(rwLock, isWrite), rwLock, shouldBlock: true, canInterrupt: false,
            blockedUntil: BlockedOperation.NotTimed, shouldRetry: false);

    public void RwLockUnlock(object rwLock, bool isWrite)
    {
        var context = CurrentThread();
        var lockContext = GetRwLockContext(rwLock, isWrite);
        if (!lockContext.IsLockHolder(context))
        {
            throw new SynchronizationLockException("Reader-writer lock is not held by the current thread.");
        }
        UnlockImpl(lockContext, context, unlockBecauseOfWait: false);
    }

    private LockContext GetRwLockContext(object rwLock, bool isWrite)
    {
        var (read, write) = GetRwLockContexts(rwLock);
        return isWrite ? write : (LockContext)read;
    }

    // ---------------------------------------------------------------------
    // Semaphores
    // ---------------------------------------------------------------------

    private SemaphoreContext GetSemaphoreContext(object semaphore, int initialPermits) =>
        _semaphoreManager.GetValue(semaphore, o => new SemaphoreContext(initialPermits, o));

    public bool SemaphoreAcquire(object semaphore, int initialPermits, int permits, bool shouldBlock,
        bool canInterrupt, long blockedUntil)
    {
        var context = CurrentThread();
        context.PendingOperation = new LockLockOperation(semaphore);
        context.State = FrayThreadState.Runnable;
        ScheduleNextOperation(true);

        var semaphoreContext = GetSemaphoreContext(semaphore, initialPermits);
        var blockingWait = shouldBlock || blockedUntil != BlockedOperation.NotTimed;
        while (!semaphoreContext.Acquire(permits, blockingWait, canInterrupt, context))
        {
            if (!blockingWait)
            {
                return false;
            }
            context.PendingOperation = new LockBlocked(blockedUntil, semaphoreContext);
            context.State = FrayThreadState.Blocked;
            ScheduleNextOperation(true);
            if (canInterrupt)
            {
                context.CheckInterrupt();
            }
            var pendingOperation = context.PendingOperation;
            VerifyOrReport(pendingOperation is ThreadResumeOperation, "Semaphore acquire resumed with unexpected operation.");
            if (pendingOperation is ThreadResumeOperation { NoTimeout: false } &&
                blockedUntil != BlockedOperation.NotTimed)
            {
                return false;
            }
        }
        return true;
    }

    public void SemaphoreRelease(object semaphore, int initialPermits, int permits) =>
        GetSemaphoreContext(semaphore, initialPermits).Release(permits);

    public int SemaphorePermits(object semaphore, int initialPermits) =>
        GetSemaphoreContext(semaphore, initialPermits).TotalPermits;

    // ---------------------------------------------------------------------
    // Countdown latches
    // ---------------------------------------------------------------------

    private CountDownLatchContext GetLatchContext(object latch, long initialCount) =>
        _latchManager.GetValue(latch, o => new CountDownLatchContext(initialCount, o));

    public bool LatchAwait(object latch, long initialCount, long blockedUntil)
    {
        var context = CurrentThread();
        var latchContext = GetLatchContext(latch, initialCount);

        context.PendingOperation = new ObjectWaitOperation(ObjectIds.Of(latch));
        context.State = FrayThreadState.Runnable;
        ScheduleNextOperation(true);

        if (latchContext.Await(true, context))
        {
            context.PendingOperation = new CountDownLatchAwaitBlocked(blockedUntil, latchContext);
            context.State = FrayThreadState.Blocked;
            CheckDeadlock(() =>
            {
                latchContext.UnblockThread(context, InterruptionType.ResourceAvailable);
                context.State = FrayThreadState.Running;
            });
            ScheduleNextOperation(true);
            context.CheckInterrupt();
            return (context.PendingOperation as ThreadResumeOperation)?.NoTimeout ?? true;
        }
        context.PendingOperation = new ThreadResumeOperation(true);
        return true;
    }

    public void LatchCountDown(object latch, long initialCount) =>
        GetLatchContext(latch, initialCount).CountDown();

    public long LatchCount(object latch, long initialCount) =>
        GetLatchContext(latch, initialCount).Count;

    // ---------------------------------------------------------------------
    // Memory operations
    // ---------------------------------------------------------------------

    public void MemoryOperation(int resourceId, MemoryOpType type)
    {
        var context = CurrentThread();
        context.PendingOperation = new MemoryOperation(resourceId, type);
        context.State = FrayThreadState.Runnable;
        ScheduleNextOperation(true);
    }

    // ---------------------------------------------------------------------
    // Scheduling
    // ---------------------------------------------------------------------

    private void ForceUnblockAllBlockedThreads()
    {
        foreach (var thread in _registeredThreads)
        {
            if (thread.State == FrayThreadState.Blocked && thread.PendingOperation is BlockedOperation blocked)
            {
                blocked.Unblock(thread, InterruptionType.Force);
            }
        }
    }

    public void ScheduleNextOperationAndCheckDeadlock(bool shouldBlockCurrentThread)
    {
        try
        {
            ScheduleNextOperation(shouldBlockCurrentThread);
        }
        catch (DeadlockException)
        {
            ForceUnblockAllBlockedThreads();
            ScheduleNextOperation(shouldBlockCurrentThread);
        }
    }

    /// <summary>Reports a deadlock when no thread can make progress, after running <paramref name="cleanUp"/>.</summary>
    public void CheckDeadlock(Action cleanUp)
    {
        var deadlock = false;
        if (_registeredThreads.All(t => !t.Schedulable()))
        {
            GetEnabledOperations();
            deadlock = _registeredThreads.All(t => !t.Schedulable());
        }
        if (deadlock)
        {
            var e = new DeadlockException();
            ReportError(e);
            cleanUp();
            throw e;
        }
    }

    private long UnblockTimedBlocking()
    {
        long blockingTime = 0;
        if (Config.IgnoreTimedBlock && _registeredThreads.Any(t => t.State == FrayThreadState.Runnable))
        {
            return 0;
        }
        var currentTime = Environment.TickCount64;
        foreach (var thread in _registeredThreads)
        {
            if (thread.PendingOperation is BlockedOperation { IsTimed: true } operation)
            {
                if (Config.IgnoreTimedBlock || operation.BlockedUntil <= currentTime)
                {
                    operation.Unblock(thread, InterruptionType.Timeout);
                }
                else
                {
                    var remaining = operation.BlockedUntil - currentTime;
                    blockingTime = blockingTime == 0 ? remaining : Math.Min(blockingTime, remaining);
                }
            }
        }
        return Config.IgnoreTimedBlock ? 0 : blockingTime;
    }

    public IReadOnlyList<ThreadContext> GetEnabledOperations()
    {
        _enabledBuffer.Clear();
        var blockingTime = UnblockTimedBlocking();
        foreach (var thread in _registeredThreads)
        {
            if (thread.State == FrayThreadState.Runnable)
            {
                _enabledBuffer.Add(thread);
            }
        }
        if (_enabledBuffer.Count == 0 && blockingTime > 0)
        {
            // Everything is blocked on timed operations: advance wall time.
            Thread.Sleep((int)Math.Min(blockingTime, int.MaxValue));
            UnblockTimedBlocking();
            foreach (var thread in _registeredThreads)
            {
                if (thread.State == FrayThreadState.Runnable)
                {
                    _enabledBuffer.Add(thread);
                }
            }
        }
        // _registeredThreads is in index order, so the buffer already is too.
        return _enabledBuffer;
    }

    /// <summary>
    /// The scheduling point: asks the scheduler for the next thread, hands
    /// execution over, and (optionally) parks the calling thread until it is
    /// picked again.
    /// </summary>
    public void ScheduleNextOperation(bool shouldBlockCurrentThread)
    {
        var currentThread = _registeredThreads[_currentThreadIndex];
        VerifyOrReport(_registeredThreads.All(t => t.State != FrayThreadState.Running),
            "No thread may be in Running state at a schedule point.");

        if (!currentThread.IsExiting &&
            _currentThreadIndex != _mainThreadIndex &&
            (BugFound != null || (_mainExiting && Config.AbortThreadsAfterMainExit)))
        {
            // A bug was found (or the main thread exited): unwind this thread.
            currentThread.State = FrayThreadState.Running;
            throw new TargetTerminateException();
        }

        var enabledOperations = GetEnabledOperations();

        if (enabledOperations.Count == 0)
        {
            if (_registeredThreads.All(t => t.State != FrayThreadState.Blocked))
            {
                // Nothing left to run: hand control back to the main thread.
                if (_currentThreadIndex != _mainThreadIndex)
                {
                    _currentThreadIndex = _mainThreadIndex;
                    _registeredThreads[_mainThreadIndex].Unblock();
                }
                return;
            }
            var deadlock = new DeadlockException();
            ReportError(deadlock);
            throw deadlock;
        }

        Step += 1;
        if (Config.MaxScheduledSteps >= 1 && Step > Config.MaxScheduledSteps &&
            !currentThread.IsExiting && currentThread.State != FrayThreadState.MainExiting)
        {
            currentThread.State = FrayThreadState.Running;
            var liveness = new LivenessException();
            ReportError(liveness);
            throw liveness;
        }

        ThreadContext nextThread;
        try
        {
            nextThread = Scheduler.ScheduleNextOperation(enabledOperations, _registeredThreads);
        }
        catch (Exception e)
        {
            ReportError(e);
            nextThread = enabledOperations[0];
        }

        foreach (var observer in Observers)
        {
            observer.OnNewSchedule(_registeredThreads, nextThread);
        }
        _currentThreadIndex = nextThread.Index;
        nextThread.State = FrayThreadState.Running;
        if (currentThread != nextThread)
        {
            foreach (var observer in Observers)
            {
                observer.OnContextSwitch(currentThread, nextThread);
            }
        }
        RunThread(currentThread, nextThread);
        if (currentThread != nextThread && shouldBlockCurrentThread)
        {
            currentThread.Block();
        }
    }

    private static void RunThread(ThreadContext currentThread, ThreadContext nextThread)
    {
        // Threads woken from a wait reacquire the lock when they resume; the
        // resume operation carries the timeout flag.
        switch (nextThread.PendingOperation)
        {
            case ObjectWakeBlocked wake:
                nextThread.PendingOperation = new ThreadResumeOperation(wake.NoTimeout);
                break;
            case ConditionWakeBlocked wake:
                nextThread.PendingOperation = new ThreadResumeOperation(wake.NoTimeout);
                break;
        }
        if (currentThread != nextThread)
        {
            nextThread.Unblock();
        }
    }
}
