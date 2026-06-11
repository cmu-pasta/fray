using Fray.Core.Contexts;

namespace Fray.Core.Operations;

/// <summary>
/// The pending operation of a controlled thread. The scheduler consults the
/// pending operation of each enabled thread to decide which one runs next.
///
/// Mirrors the <c>org.pastalab.fray.core.concurrency.operations</c> hierarchy.
/// </summary>
public abstract class Operation
{
    public override string ToString() => GetType().Name;
}

/// <summary>An operation that can never participate in a data race.</summary>
public abstract class NonRacingOperation : Operation { }

/// <summary>
/// An operation that may race with operations of other threads on the same
/// resource. Used by partial-order aware schedulers such as POS.
/// </summary>
public abstract class RacingOperation : Operation
{
    public int Resource { get; }
    public MemoryOpType Type { get; }

    protected RacingOperation(int resource, MemoryOpType type)
    {
        Resource = resource;
        Type = type;
    }

    public abstract bool IsRacing(Operation op);

    public override string ToString() => $"{base.ToString()}@{Resource}:{Type}";
}

/// <summary>The thread is executing regular (non-synchronizing) code.</summary>
public sealed class RunningOperation : NonRacingOperation { }

/// <summary>The thread has been created but not scheduled for the first time yet.</summary>
public sealed class ThreadStartOperation : NonRacingOperation
{
    public int ParentIndex { get; }

    public ThreadStartOperation(int parentIndex) => ParentIndex = parentIndex;
}

/// <summary>The thread was unblocked and will resume its interrupted primitive.</summary>
public sealed class ThreadResumeOperation : NonRacingOperation
{
    public bool NoTimeout { get; }

    public ThreadResumeOperation(bool noTimeout) => NoTimeout = noTimeout;
}

/// <summary>A read or write to controlled shared memory.</summary>
public sealed class MemoryOperation : RacingOperation
{
    public MemoryOperation(int resource, MemoryOpType type) : base(resource, type) { }

    public override bool IsRacing(Operation op) =>
        op is MemoryOperation other &&
        other.Resource == Resource &&
        (Type == MemoryOpType.MemoryWrite || other.Type == MemoryOpType.MemoryWrite);
}

/// <summary>The thread is about to acquire a lock.</summary>
public sealed class LockLockOperation : RacingOperation
{
    public string Name { get; }

    public LockLockOperation(object lockObject)
        : base(ObjectIds.Of(lockObject), MemoryOpType.MemoryWrite)
    {
        Name = lockObject.GetType().Name;
    }

    public override bool IsRacing(Operation op) =>
        op is LockLockOperation other && other.Resource == Resource;

    public override string ToString() => $"{GetType().Name}@{Name}:{Type}";
}

/// <summary>The thread is about to wait on a monitor / condition / latch.</summary>
public sealed class ObjectWaitOperation : RacingOperation
{
    public ObjectWaitOperation(int resource) : base(resource, MemoryOpType.MemoryWrite) { }

    public override bool IsRacing(Operation op) =>
        op is ObjectWaitOperation other && other.Resource == Resource;
}

/// <summary>
/// Base class for the pending operation of a blocked thread. Knows how to
/// unblock the thread (timeout, interrupt, forced wakeup, resource available).
/// </summary>
public abstract class BlockedOperation : NonRacingOperation
{
    public const long NotTimed = -1;

    public ResourceInfo ResourceInfo { get; }
    public long BlockedUntil { get; }

    protected BlockedOperation(ResourceInfo resourceInfo, long blockedUntil)
    {
        ResourceInfo = resourceInfo;
        BlockedUntil = blockedUntil;
    }

    public bool IsTimed => BlockedUntil != NotTimed;

    /// <summary>Unblocks the given thread that is blocked on this operation.</summary>
    public abstract void Unblock(ThreadContext thread, InterruptionType type);
}

/// <summary>Blocked acquiring a lock, semaphore, or similar acquirable resource.</summary>
public sealed class LockBlocked : BlockedOperation
{
    private readonly IInterruptibleContext _context;

    public LockBlocked(long blockedUntil, Acquirable acquirable)
        : base(acquirable.ResourceInfo, blockedUntil)
    {
        _context = (IInterruptibleContext)acquirable;
    }

    public override void Unblock(ThreadContext thread, InterruptionType type) =>
        _context.UnblockThread(thread, type);
}

/// <summary>Blocked in a monitor wait (<see cref="Fray.FrayMonitor"/>).</summary>
public sealed class ObjectWaitBlocked : BlockedOperation
{
    public ObjectNotifyContext ObjectContext { get; }

    public ObjectWaitBlocked(ObjectNotifyContext objectContext, long blockedUntil)
        : base(new ResourceInfo(objectContext.LockContext.ResourceInfo.Id, ResourceType.Condition), blockedUntil)
    {
        ObjectContext = objectContext;
    }

    public override void Unblock(ThreadContext thread, InterruptionType type) =>
        ObjectContext.UnblockThread(thread, type);
}

/// <summary>Woken from a monitor wait, but still waiting to reacquire the lock.</summary>
public sealed class ObjectWakeBlocked : BlockedOperation
{
    public ObjectNotifyContext ObjectContext { get; }
    public bool NoTimeout { get; }

    public ObjectWakeBlocked(ObjectNotifyContext objectContext, bool noTimeout)
        : base(new ResourceInfo(objectContext.LockContext.ResourceInfo.Id, ResourceType.Condition), NotTimed)
    {
        ObjectContext = objectContext;
        NoTimeout = noTimeout;
    }

    public override void Unblock(ThreadContext thread, InterruptionType type)
    {
        // Only a forced wakeup (deadlock recovery / wind-down) may resume the
        // thread before the lock is reacquirable; the resume path retries.
        if (type == InterruptionType.Force)
        {
            thread.PendingOperation = new ThreadResumeOperation(NoTimeout);
            thread.State = FrayThreadState.Runnable;
        }
    }
}

/// <summary>Blocked in a condition await (<see cref="Fray.FrayCondition"/>).</summary>
public sealed class ConditionAwaitBlocked : BlockedOperation
{
    public ConditionSignalContext ConditionContext { get; }
    public bool CanInterrupt { get; }

    public ConditionAwaitBlocked(ConditionSignalContext conditionContext, bool canInterrupt, long blockedUntil)
        : base(new ResourceInfo(conditionContext.ResourceId, ResourceType.Condition), blockedUntil)
    {
        ConditionContext = conditionContext;
        CanInterrupt = canInterrupt;
    }

    public override void Unblock(ThreadContext thread, InterruptionType type) =>
        ConditionContext.UnblockThread(thread, type);
}

/// <summary>Woken from a condition await, but still waiting to reacquire the lock.</summary>
public sealed class ConditionWakeBlocked : BlockedOperation
{
    public ConditionSignalContext ConditionContext { get; }
    public bool NoTimeout { get; }

    public ConditionWakeBlocked(ConditionSignalContext conditionContext, bool noTimeout)
        : base(new ResourceInfo(conditionContext.ResourceId, ResourceType.Condition), NotTimed)
    {
        ConditionContext = conditionContext;
        NoTimeout = noTimeout;
    }

    public override void Unblock(ThreadContext thread, InterruptionType type)
    {
        if (type == InterruptionType.Force)
        {
            thread.PendingOperation = new ThreadResumeOperation(NoTimeout);
            thread.State = FrayThreadState.Runnable;
        }
    }
}

/// <summary>Blocked awaiting a countdown latch (<see cref="Fray.FrayCountdownEvent"/>).</summary>
public sealed class CountDownLatchAwaitBlocked : BlockedOperation
{
    public CountDownLatchContext LatchContext { get; }

    public CountDownLatchAwaitBlocked(long blockedUntil, CountDownLatchContext latchContext)
        : base(new ResourceInfo(latchContext.LatchId, ResourceType.CountdownLatch), blockedUntil)
    {
        LatchContext = latchContext;
    }

    public override void Unblock(ThreadContext thread, InterruptionType type) =>
        LatchContext.UnblockThread(thread, type);
}

/// <summary>Blocked in a controlled sleep.</summary>
public sealed class SleepBlocked : BlockedOperation
{
    private readonly ThreadContext _threadContext;

    public SleepBlocked(ThreadContext threadContext, long blockedUntil)
        : base(new ResourceInfo(0, ResourceType.Sleep), blockedUntil)
    {
        _threadContext = threadContext;
    }

    public override void Unblock(ThreadContext thread, InterruptionType type)
    {
        _threadContext.PendingOperation = new ThreadResumeOperation(true);
        _threadContext.State = FrayThreadState.Runnable;
    }
}
