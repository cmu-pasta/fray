using Fray.Core.Operations;

namespace Fray.Core.Contexts;

/// <summary>
/// Models a counting semaphore.
///
/// Mirrors <c>org.pastalab.fray.core.concurrency.context.SemaphoreContext</c>.
/// </summary>
public sealed class SemaphoreContext : Acquirable, IInterruptibleContext
{
    private readonly Dictionary<int, (int Permits, LockWaiter Waiter)> _lockWaiters = new();

    public int TotalPermits { get; private set; }

    public SemaphoreContext(int totalPermits, object semaphore)
        : base(new ResourceInfo(ObjectIds.Of(semaphore), ResourceType.Semaphore))
    {
        TotalPermits = totalPermits;
    }

    public bool Acquire(int permits, bool shouldBlock, bool canInterrupt, ThreadContext thread)
    {
        if (TotalPermits >= permits)
        {
            TotalPermits -= permits;
            thread.AcquiredResources.Add(this);
            return true;
        }
        if (canInterrupt)
        {
            thread.CheckInterrupt();
        }
        if (shouldBlock)
        {
            _lockWaiters[thread.Index] = (permits, new LockWaiter(canInterrupt, thread));
        }
        return false;
    }

    public void Release(int permits)
    {
        TotalPermits += permits;
        if (TotalPermits <= 0)
        {
            return;
        }
        foreach (var (index, entry) in _lockWaiters.ToList())
        {
            if (TotalPermits >= entry.Permits)
            {
                entry.Waiter.Thread.PendingOperation = new ThreadResumeOperation(true);
                entry.Waiter.Thread.State = FrayThreadState.Runnable;
                _lockWaiters.Remove(index);
            }
        }
    }

    public int DrainPermits()
    {
        var permits = TotalPermits;
        TotalPermits = 0;
        return permits;
    }

    public bool UnblockThread(ThreadContext thread, InterruptionType type)
    {
        if (!_lockWaiters.TryGetValue(thread.Index, out var entry))
        {
            return false;
        }
        var noTimeout = type != InterruptionType.Timeout;
        if (entry.Waiter.CanInterrupt || type == InterruptionType.Force || type == InterruptionType.Timeout)
        {
            entry.Waiter.Thread.PendingOperation = new ThreadResumeOperation(noTimeout);
            entry.Waiter.Thread.State = FrayThreadState.Runnable;
            _lockWaiters.Remove(thread.Index);
        }
        return false;
    }
}
