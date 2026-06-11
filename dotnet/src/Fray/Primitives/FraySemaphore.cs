using Fray.Core.Operations;

namespace Fray;

/// <summary>
/// Controlled counting semaphore, equivalent to Java's <c>Semaphore</c>.
/// </summary>
public sealed class FraySemaphore
{
    private readonly object _real = new();
    private readonly int _initialPermits;
    private int _permits;

    public FraySemaphore(int permits)
    {
        _initialPermits = permits;
        _permits = permits;
    }

    public void Acquire(int permits = 1)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            lock (_real)
            {
                while (_permits < permits)
                {
                    Monitor.Wait(_real);
                }
                _permits -= permits;
            }
            return;
        }
        runContext.SemaphoreAcquire(this, _initialPermits, permits, shouldBlock: true,
            canInterrupt: true, BlockedOperation.NotTimed);
    }

    public bool TryAcquire(int permits = 1)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            lock (_real)
            {
                if (_permits < permits)
                {
                    return false;
                }
                _permits -= permits;
                return true;
            }
        }
        return runContext.SemaphoreAcquire(this, _initialPermits, permits, shouldBlock: false,
            canInterrupt: false, BlockedOperation.NotTimed);
    }

    public void Release(int permits = 1)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            lock (_real)
            {
                _permits += permits;
                Monitor.PulseAll(_real);
            }
            return;
        }
        runContext.SemaphoreRelease(this, _initialPermits, permits);
    }

    public int AvailablePermits
    {
        get
        {
            var runContext = FrayRuntime.ControlledContext();
            if (runContext == null)
            {
                lock (_real)
                {
                    return _permits;
                }
            }
            return runContext.SemaphorePermits(this, _initialPermits);
        }
    }
}
