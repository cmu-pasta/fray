using Fray.Core.Operations;

namespace Fray;

/// <summary>
/// Controlled reentrant mutual-exclusion lock with condition variables,
/// equivalent to Java's <c>ReentrantLock</c>. Outside of Fray it delegates to
/// a private monitor.
/// </summary>
public sealed class FrayLock
{
    private readonly object _real = new();

    internal object RealLock => _real;

    public void Lock()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Monitor.Enter(_real);
            return;
        }
        runContext.LockLock(this, canInterrupt: false);
    }

    /// <summary>Like <see cref="Lock"/>, but responds to <see cref="FrayThread.Interrupt"/>.</summary>
    public void LockInterruptibly()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Monitor.Enter(_real);
            return;
        }
        runContext.LockLock(this, canInterrupt: true);
    }

    public bool TryLock()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return Monitor.TryEnter(_real);
        }
        return runContext.LockTryLock(this, canInterrupt: false, BlockedOperation.NotTimed);
    }

    public bool TryLock(int millisecondsTimeout)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return Monitor.TryEnter(_real, millisecondsTimeout);
        }
        return runContext.LockTryLock(this, canInterrupt: true, FrayMonitor.TimeoutToDeadline(millisecondsTimeout));
    }

    public void Unlock()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Monitor.Exit(_real);
            return;
        }
        runContext.LockUnlock(this);
    }

    /// <summary>Runs <paramref name="action"/> while holding the lock.</summary>
    public void WithLock(Action action)
    {
        Lock();
        try
        {
            action();
        }
        finally
        {
            Unlock();
        }
    }

    public FrayCondition NewCondition() => new(this);
}

/// <summary>
/// A condition variable bound to a <see cref="FrayLock"/>, equivalent to
/// Java's <c>Condition</c>. The owning lock must be held to await or signal.
/// </summary>
public sealed class FrayCondition
{
    private readonly FrayLock _lock;

    internal FrayCondition(FrayLock owner) => _lock = owner;

    /// <summary>Waits for a signal; returns false when the timeout elapsed first.</summary>
    public bool Await(int millisecondsTimeout = Timeout.Infinite)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            // Passthrough conditions share the lock's monitor: signals on
            // sibling conditions surface as spurious wakeups, which callers
            // must tolerate anyway.
            return Monitor.Wait(_lock.RealLock, millisecondsTimeout);
        }
        return runContext.ConditionAwait(this, _lock, FrayMonitor.TimeoutToDeadline(millisecondsTimeout), canInterrupt: true);
    }

    public void Signal()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Monitor.Pulse(_lock.RealLock);
            return;
        }
        runContext.ConditionSignal(this, _lock, all: false);
    }

    public void SignalAll()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Monitor.PulseAll(_lock.RealLock);
            return;
        }
        runContext.ConditionSignal(this, _lock, all: true);
    }
}
