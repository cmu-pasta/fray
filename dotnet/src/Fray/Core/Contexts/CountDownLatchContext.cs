using Fray.Core.Operations;

namespace Fray.Core.Contexts;

/// <summary>
/// Models a countdown latch (<see cref="Fray.FrayCountdownEvent"/>).
///
/// Mirrors <c>org.pastalab.fray.core.concurrency.context.CountDownLatchContext</c>.
/// </summary>
public sealed class CountDownLatchContext : IInterruptibleContext
{
    private readonly Dictionary<int, LockWaiter> _latchWaiters = new();

    public int LatchId { get; }
    public long Count { get; private set; }

    public CountDownLatchContext(long count, object latch)
    {
        LatchId = ObjectIds.Of(latch);
        Count = count;
    }

    /// <summary>Returns true when the calling thread has to block.</summary>
    public bool Await(bool canInterrupt, ThreadContext thread)
    {
        if (Count > 0)
        {
            if (canInterrupt)
            {
                thread.CheckInterrupt();
            }
            _latchWaiters[thread.Index] = new LockWaiter(canInterrupt, thread);
            return true;
        }
        return false;
    }

    /// <summary>Returns the number of threads unblocked by this countdown.</summary>
    public int CountDown()
    {
        if (Count == 0)
        {
            return 0;
        }
        Count -= 1;
        if (Count != 0)
        {
            return 0;
        }
        var unblocked = 0;
        foreach (var waiter in _latchWaiters.Values.ToList())
        {
            if (UnblockThread(waiter.Thread, InterruptionType.ResourceAvailable))
            {
                unblocked += 1;
            }
        }
        return unblocked;
    }

    public bool UnblockThread(ThreadContext thread, InterruptionType type)
    {
        if (type == InterruptionType.Force)
        {
            // Forced wakeup (deadlock recovery): open the latch entirely.
            while (Count > 0)
            {
                CountDown();
            }
            return false;
        }
        if (!_latchWaiters.TryGetValue(thread.Index, out var waiter))
        {
            return false;
        }
        if (type == InterruptionType.Interrupt && !waiter.CanInterrupt)
        {
            return false;
        }
        waiter.Thread.PendingOperation = new ThreadResumeOperation(type != InterruptionType.Timeout);
        waiter.Thread.State = FrayThreadState.Runnable;
        _latchWaiters.Remove(thread.Index);
        return true;
    }
}
