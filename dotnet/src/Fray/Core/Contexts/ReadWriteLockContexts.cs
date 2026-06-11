using Fray.Core.Operations;

namespace Fray.Core.Contexts;

/// <summary>
/// Read side of a reader-writer lock. Multiple readers may hold the lock as
/// long as no writer does.
///
/// Mirrors <c>org.pastalab.fray.core.concurrency.context.ReadLockContext</c>.
/// </summary>
public sealed class ReadLockContext : LockContext
{
    private readonly Dictionary<int, LockWaiter> _lockWaiters = new();
    private readonly Dictionary<int, int> _lockTimes = new();
    internal readonly HashSet<int> LockHolders = new();

    public WriteLockContext WriteLockContext { get; internal set; } = null!;

    public ReadLockContext(object lockObject)
        : base(new ResourceInfo(ObjectIds.Of(lockObject), ResourceType.ReaderWriterLock)) { }

    public override IDictionary<int, ThreadContext> WakingThreads { get; } = new Dictionary<int, ThreadContext>();

    public override ISet<SignalContext> SignalContexts { get; } = new HashSet<SignalContext>();

    public override void AddWakingThread(ThreadContext thread) { }

    public override bool CanLock(ThreadContext thread) =>
        CanLockInternal(thread) && WriteLockContext.CanLockInternal(thread);

    internal bool CanLockInternal(ThreadContext thread) =>
        LockHolders.Count == 0 || LockHolders.Contains(thread.Index);

    public override bool Lock(ThreadContext lockThread, bool shouldBlock, bool lockBecauseOfWait, bool canInterrupt)
    {
        if (!WriteLockContext.CanLockInternal(lockThread))
        {
            if (canInterrupt)
            {
                lockThread.CheckInterrupt();
            }
            if (shouldBlock)
            {
                _lockWaiters[lockThread.Index] = new LockWaiter(canInterrupt, lockThread);
            }
            return false;
        }
        _lockTimes[lockThread.Index] = _lockTimes.GetValueOrDefault(lockThread.Index) + 1;
        LockHolders.Add(lockThread.Index);
        lockThread.AcquiredResources.Add(this);
        return true;
    }

    public override bool Unlock(ThreadContext lockThread, bool unlockBecauseOfWait, bool earlyExit)
    {
        if (!LockHolders.Contains(lockThread.Index))
        {
            return false;
        }
        _lockTimes[lockThread.Index] -= 1;
        if (_lockTimes[lockThread.Index] == 0)
        {
            _lockTimes.Remove(lockThread.Index);
            LockHolders.Remove(lockThread.Index);
            if (LockHolders.Count == 0 && !WriteLockContext.IsHeld)
            {
                WriteLockContext.UnlockWaiters();
                UnlockWaiters();
                lockThread.AcquiredResources.Remove(this);
                return true;
            }
        }
        return false;
    }

    internal void UnlockWaiters()
    {
        foreach (var waiter in _lockWaiters.Values.ToList())
        {
            UnblockThread(waiter.Thread, InterruptionType.ResourceAvailable);
        }
    }

    public override bool IsLockHolder(ThreadContext thread) =>
        WriteLockContext.IsLockHolderInternal(thread) || LockHolders.Contains(thread.Index);

    public override bool UnblockThread(ThreadContext thread, InterruptionType type)
    {
        if (!_lockWaiters.TryGetValue(thread.Index, out var waiter))
        {
            return false;
        }
        if ((waiter.CanInterrupt && type == InterruptionType.Interrupt) ||
            type is InterruptionType.Force or InterruptionType.ResourceAvailable or InterruptionType.Timeout)
        {
            waiter.Thread.PendingOperation = new ThreadResumeOperation(type != InterruptionType.Timeout);
            waiter.Thread.State = FrayThreadState.Runnable;
            _lockWaiters.Remove(thread.Index);
        }
        return false;
    }
}

/// <summary>
/// Write side of a reader-writer lock.
///
/// Mirrors <c>org.pastalab.fray.core.concurrency.context.WriteLockContext</c>.
/// </summary>
public sealed class WriteLockContext : LockContext
{
    private readonly Dictionary<int, LockWaiter> _lockWaiters = new();
    private readonly Dictionary<int, int> _lockTimes = new();
    private ThreadContext? _lockHolder;

    public ReadLockContext ReadLockContext { get; internal set; } = null!;

    public WriteLockContext(object lockObject)
        : base(new ResourceInfo(ObjectIds.Of(lockObject), ResourceType.ReaderWriterLock)) { }

    public override IDictionary<int, ThreadContext> WakingThreads { get; } = new Dictionary<int, ThreadContext>();

    public override ISet<SignalContext> SignalContexts { get; } = new HashSet<SignalContext>();

    internal bool IsHeld => _lockHolder != null;

    public override void AddWakingThread(ThreadContext thread) => WakingThreads[thread.Index] = thread;

    public override bool CanLock(ThreadContext thread) =>
        CanLockInternal(thread) && ReadLockContext.CanLockInternal(thread);

    internal bool CanLockInternal(ThreadContext thread) => _lockHolder == null || _lockHolder == thread;

    public override bool Lock(ThreadContext lockThread, bool shouldBlock, bool lockBecauseOfWait, bool canInterrupt)
    {
        if (!CanLock(lockThread))
        {
            if (canInterrupt)
            {
                lockThread.CheckInterrupt();
            }
            if (shouldBlock)
            {
                _lockWaiters[lockThread.Index] = new LockWaiter(canInterrupt, lockThread);
            }
            return false;
        }
        if (!lockBecauseOfWait)
        {
            _lockTimes[lockThread.Index] = _lockTimes.GetValueOrDefault(lockThread.Index) + 1;
        }
        _lockHolder = lockThread;
        lockThread.AcquiredResources.Add(this);
        return true;
    }

    public override bool Unlock(ThreadContext lockThread, bool unlockBecauseOfWait, bool earlyExit)
    {
        if (_lockHolder != lockThread)
        {
            return false;
        }
        if (!unlockBecauseOfWait)
        {
            _lockTimes[lockThread.Index] -= 1;
        }
        if (_lockTimes.GetValueOrDefault(lockThread.Index) == 0 || unlockBecauseOfWait)
        {
            if (_lockTimes.GetValueOrDefault(lockThread.Index) == 0)
            {
                _lockTimes.Remove(lockThread.Index);
            }
            _lockHolder = null;
            ReadLockContext.UnlockWaiters();
            if (ReadLockContext.LockHolders.Count == 0)
            {
                UnlockWaiters();
                lockThread.AcquiredResources.Remove(this);
                return true;
            }
        }
        return false;
    }

    public override bool IsLockHolder(ThreadContext thread) =>
        IsLockHolderInternal(thread) || ReadLockContext.LockHolders.Contains(thread.Index);

    internal bool IsLockHolderInternal(ThreadContext thread) => _lockHolder == thread;

    public override bool UnblockThread(ThreadContext thread, InterruptionType type)
    {
        if (!_lockWaiters.TryGetValue(thread.Index, out var waiter))
        {
            return false;
        }
        if ((waiter.CanInterrupt && type == InterruptionType.Interrupt) ||
            type is InterruptionType.Force or InterruptionType.ResourceAvailable or InterruptionType.Timeout)
        {
            waiter.Thread.PendingOperation = new ThreadResumeOperation(type != InterruptionType.Timeout);
            waiter.Thread.State = FrayThreadState.Runnable;
            _lockWaiters.Remove(thread.Index);
        }
        return false;
    }

    internal void UnlockWaiters()
    {
        foreach (var waiter in _lockWaiters.Values.ToList())
        {
            UnblockThread(waiter.Thread, InterruptionType.ResourceAvailable);
        }
        // Waking threads are write waiters as well.
        foreach (var thread in WakingThreads.Values)
        {
            thread.PendingOperation = new ThreadResumeOperation(true);
            thread.State = FrayThreadState.Runnable;
        }
    }
}
