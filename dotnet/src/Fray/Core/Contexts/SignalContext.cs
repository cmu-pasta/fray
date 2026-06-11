using Fray.Core.Operations;
using Fray.Core.Randomness;

namespace Fray.Core.Contexts;

/// <summary>
/// Models a wait/notify queue attached to a lock: either a monitor wait set
/// (<see cref="ObjectNotifyContext"/>) or a condition variable
/// (<see cref="ConditionSignalContext"/>).
///
/// Mirrors <c>org.pastalab.fray.core.concurrency.context.SignalContext</c>.
/// </summary>
public abstract class SignalContext : IInterruptibleContext
{
    public LockContext LockContext { get; }
    public List<ThreadContext> WaitingThreads { get; } = new();

    protected SignalContext(LockContext lockContext) => LockContext = lockContext;

    protected abstract void OnUnblock(ThreadContext threadContext, InterruptionType type);

    protected abstract void OnBlock(ThreadContext threadContext, long blockedUntil, bool canInterrupt);

    public void AddWaitingThread(ThreadContext threadContext, long blockedUntil, bool canInterrupt)
    {
        threadContext.RunContext.VerifyOrReport(!WaitingThreads.Contains(threadContext),
            "thread is already waiting on this signal context");
        WaitingThreads.Add(threadContext);
        OnBlock(threadContext, blockedUntil, canInterrupt);
    }

    public bool UnblockThread(ThreadContext thread, InterruptionType type)
    {
        var threadContext = WaitingThreads.FirstOrDefault(t => t == thread);
        if (threadContext == null)
        {
            return false;
        }
        WaitingThreads.Remove(threadContext);
        OnUnblock(threadContext, type);
        LockContext.AddWakingThread(threadContext);
        if (LockContext.CanLock(thread))
        {
            threadContext.State = FrayThreadState.Runnable;
            return true;
        }
        return false;
    }

    /// <summary>Wakes one (random) or all waiting threads.</summary>
    public void Signal(IRandomness randomness, bool all)
    {
        if (WaitingThreads.Count == 0)
        {
            return;
        }
        if (all)
        {
            foreach (var thread in WaitingThreads.ToList())
            {
                UnblockThread(thread, InterruptionType.ResourceAvailable);
            }
        }
        else
        {
            var index = randomness.NextInt() % WaitingThreads.Count;
            UnblockThread(WaitingThreads[index], InterruptionType.ResourceAvailable);
        }
    }
}

/// <summary>Wait set of a monitor object (<see cref="Fray.FrayMonitor"/> Wait/Pulse).</summary>
public sealed class ObjectNotifyContext : SignalContext
{
    public ObjectNotifyContext(LockContext lockContext) : base(lockContext) { }

    protected override void OnUnblock(ThreadContext threadContext, InterruptionType type) =>
        threadContext.PendingOperation = new ObjectWakeBlocked(this, type != InterruptionType.Timeout);

    protected override void OnBlock(ThreadContext threadContext, long blockedUntil, bool canInterrupt)
    {
        threadContext.PendingOperation = new ObjectWaitBlocked(this, blockedUntil);
        threadContext.State = FrayThreadState.Blocked;
    }
}

/// <summary>A condition variable created from a <see cref="Fray.FrayLock"/>.</summary>
public sealed class ConditionSignalContext : SignalContext
{
    public int ResourceId { get; }

    public ConditionSignalContext(LockContext lockContext, object condition) : base(lockContext)
    {
        ResourceId = ObjectIds.Of(condition);
    }

    protected override void OnUnblock(ThreadContext threadContext, InterruptionType type) =>
        threadContext.PendingOperation = new ConditionWakeBlocked(this, type != InterruptionType.Timeout);

    protected override void OnBlock(ThreadContext threadContext, long blockedUntil, bool canInterrupt)
    {
        threadContext.PendingOperation = new ConditionAwaitBlocked(this, canInterrupt, blockedUntil);
        threadContext.State = FrayThreadState.Blocked;
    }
}
