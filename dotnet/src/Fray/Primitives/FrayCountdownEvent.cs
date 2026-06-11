using Fray.Core.Operations;

namespace Fray;

/// <summary>
/// Controlled countdown latch, equivalent to Java's <c>CountDownLatch</c> /
/// .NET's <see cref="CountdownEvent"/>.
/// </summary>
public sealed class FrayCountdownEvent
{
    private readonly object _real = new();
    private readonly long _initialCount;
    private long _count;

    public FrayCountdownEvent(int count)
    {
        _initialCount = count;
        _count = count;
    }

    /// <summary>Blocks until the count reaches zero.</summary>
    public void Wait()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            lock (_real)
            {
                while (_count > 0)
                {
                    Monitor.Wait(_real);
                }
            }
            return;
        }
        runContext.LatchAwait(this, _initialCount, BlockedOperation.NotTimed);
    }

    /// <summary>Blocks until the count reaches zero; false when the timeout elapsed first.</summary>
    public bool Wait(int millisecondsTimeout)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            lock (_real)
            {
                var deadline = Environment.TickCount64 + millisecondsTimeout;
                while (_count > 0)
                {
                    var remaining = deadline - Environment.TickCount64;
                    if (remaining <= 0 || !Monitor.Wait(_real, (int)remaining))
                    {
                        return _count == 0;
                    }
                }
                return true;
            }
        }
        return runContext.LatchAwait(this, _initialCount, FrayMonitor.TimeoutToDeadline(millisecondsTimeout));
    }

    /// <summary>Decrements the count, releasing waiters when it reaches zero.</summary>
    public void Signal()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            lock (_real)
            {
                if (_count > 0 && --_count == 0)
                {
                    Monitor.PulseAll(_real);
                }
            }
            return;
        }
        runContext.LatchCountDown(this, _initialCount);
    }

    public long CurrentCount
    {
        get
        {
            var runContext = FrayRuntime.ControlledContext();
            if (runContext == null)
            {
                lock (_real)
                {
                    return _count;
                }
            }
            return runContext.LatchCount(this, _initialCount);
        }
    }
}
