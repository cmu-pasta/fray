using Fray.Core.Operations;

namespace Fray.Core.Contexts;

/// <summary>
/// Models the state of a (reentrant) mutual-exclusion lock. Fray serializes all
/// controlled threads, so the model is the single source of truth for lock
/// ownership; no underlying OS lock is acquired while a test is controlled.
///
/// Mirrors <c>org.pastalab.fray.core.concurrency.context.LockContext</c>.
/// </summary>
public abstract class LockContext : Acquirable, IInterruptibleContext
{
    protected LockContext(object lockObject)
        : base(new ResourceInfo(ObjectIds.Of(lockObject), ResourceType.Lock)) { }

    protected LockContext(ResourceInfo resourceInfo) : base(resourceInfo) { }

    /// <summary>Threads woken from a wait on this lock that must reacquire it before resuming.</summary>
    public abstract IDictionary<int, ThreadContext> WakingThreads { get; }

    /// <summary>Wait/notify contexts associated with this lock.</summary>
    public abstract ISet<SignalContext> SignalContexts { get; }

    public abstract void AddWakingThread(ThreadContext thread);

    public abstract bool CanLock(ThreadContext thread);

    /// <summary>
    /// Tries to acquire the lock for <paramref name="lockThread"/>. When the lock is
    /// unavailable and <paramref name="shouldBlock"/> is set, the thread is registered
    /// as a waiter. Returns whether the acquisition succeeded.
    /// </summary>
    public abstract bool Lock(ThreadContext lockThread, bool shouldBlock, bool lockBecauseOfWait, bool canInterrupt);

    /// <summary>
    /// Releases the lock (or one level of reentrancy). Returns whether the lock
    /// was fully released, which makes waiting threads runnable again.
    /// </summary>
    public abstract bool Unlock(ThreadContext lockThread, bool unlockBecauseOfWait, bool earlyExit);

    public abstract bool IsLockHolder(ThreadContext thread);

    public abstract bool UnblockThread(ThreadContext thread, InterruptionType type);
}

/// <summary>
/// A reentrant lock context backing both <see cref="Fray.FrayLock"/> and monitor
/// (<see cref="Fray.FrayMonitor"/>) semantics.
///
/// Mirrors <c>org.pastalab.fray.core.concurrency.context.ReentrantLockContext</c>.
/// </summary>
public sealed class ReentrantLockContext : LockContext
{
    private ThreadContext? _lockHolder;
    private readonly Dictionary<int, int> _lockTimes = new();
    private readonly Dictionary<int, LockWaiter> _lockWaiters = new();

    public ReentrantLockContext(object lockObject) : base(lockObject) { }

    public override IDictionary<int, ThreadContext> WakingThreads { get; } = new Dictionary<int, ThreadContext>();

    public override ISet<SignalContext> SignalContexts { get; } = new HashSet<SignalContext>();

    public override void AddWakingThread(ThreadContext thread) => WakingThreads[thread.Index] = thread;

    public override bool CanLock(ThreadContext thread)
    {
        ReleaseStaleHolder(thread);
        return _lockHolder == null || _lockHolder == thread;
    }

    /// <summary>
    /// During wind-down (bug found / main exited), a thread that terminated
    /// while holding this lock will never release it; treat it as released so
    /// the remaining threads can be drained. During normal execution a leaked
    /// lock must stay held to surface as an honest deadlock.
    /// </summary>
    private void ReleaseStaleHolder(ThreadContext requester)
    {
        if (_lockHolder != null &&
            _lockHolder.State == FrayThreadState.Completed &&
            requester.RunContext.IsWindingDown)
        {
            _lockTimes.Remove(_lockHolder.Index);
            _lockHolder = null;
        }
    }

    public override bool Lock(ThreadContext lockThread, bool shouldBlock, bool lockBecauseOfWait, bool canInterrupt)
    {
        ReleaseStaleHolder(lockThread);
        if (_lockHolder == null || _lockHolder == lockThread)
        {
            _lockHolder = lockThread;
            if (!lockBecauseOfWait)
            {
                _lockTimes[lockThread.Index] = _lockTimes.GetValueOrDefault(lockThread.Index) + 1;
            }
            WakingThreads.Remove(lockThread.Index);

            // Threads woken by a notify cannot resume until the lock is free again.
            foreach (var thread in WakingThreads.Values)
            {
                thread.State = FrayThreadState.Blocked;
            }
            lockThread.AcquiredResources.Add(this);
            return true;
        }

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
            foreach (var thread in WakingThreads.Values)
            {
                if (thread.State != FrayThreadState.Completed)
                {
                    thread.State = FrayThreadState.Runnable;
                }
            }
            foreach (var waiter in _lockWaiters.Values)
            {
                if (waiter.Thread.State != FrayThreadState.Completed)
                {
                    waiter.Thread.PendingOperation = new ThreadResumeOperation(true);
                    waiter.Thread.State = FrayThreadState.Runnable;
                }
            }
            _lockWaiters.Clear();
            lockThread.AcquiredResources.Remove(this);
            return true;
        }
        return false;
    }

    public override bool IsLockHolder(ThreadContext thread) => _lockHolder == thread;

    public override bool UnblockThread(ThreadContext thread, InterruptionType type)
    {
        if (!_lockWaiters.TryGetValue(thread.Index, out var waiter))
        {
            return false;
        }
        if ((waiter.CanInterrupt && type == InterruptionType.Interrupt) ||
            type == InterruptionType.Force ||
            type == InterruptionType.Timeout)
        {
            waiter.Thread.PendingOperation = new ThreadResumeOperation(type != InterruptionType.Timeout);
            waiter.Thread.State = FrayThreadState.Runnable;
            _lockWaiters.Remove(thread.Index);
        }
        return false;
    }
}
