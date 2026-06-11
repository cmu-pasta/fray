namespace Fray;

/// <summary>
/// Controlled reader-writer lock, equivalent to Java's
/// <c>ReentrantReadWriteLock</c> / .NET's <see cref="ReaderWriterLockSlim"/>.
/// </summary>
public sealed class FrayReaderWriterLock
{
    private readonly ReaderWriterLockSlim _real = new(LockRecursionPolicy.SupportsRecursion);

    public void EnterReadLock()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            _real.EnterReadLock();
            return;
        }
        runContext.RwLockLock(this, isWrite: false);
    }

    public void ExitReadLock()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            _real.ExitReadLock();
            return;
        }
        runContext.RwLockUnlock(this, isWrite: false);
    }

    public void EnterWriteLock()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            _real.EnterWriteLock();
            return;
        }
        runContext.RwLockLock(this, isWrite: true);
    }

    public void ExitWriteLock()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            _real.ExitWriteLock();
            return;
        }
        runContext.RwLockUnlock(this, isWrite: true);
    }
}
