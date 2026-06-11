using System.Runtime.CompilerServices;

namespace Fray.Core.Contexts;

public enum ResourceType
{
    Lock,
    Condition,
    Semaphore,
    CountdownLatch,
    ReaderWriterLock,
    Sleep,
}

/// <summary>Identifies the resource a thread is blocked on, for reports.</summary>
public readonly record struct ResourceInfo(int Id, ResourceType Type);

/// <summary>Stable identity hash for arbitrary objects (Java's <c>System.identityHashCode</c>).</summary>
public static class ObjectIds
{
    public static int Of(object obj) => RuntimeHelpers.GetHashCode(obj);
}

/// <summary>A resource that threads can acquire (lock, semaphore, ...).</summary>
public abstract class Acquirable
{
    public ResourceInfo ResourceInfo { get; }

    protected Acquirable(ResourceInfo resourceInfo) => ResourceInfo = resourceInfo;
}

/// <summary>A context whose blocked threads can be woken (interrupt, timeout, force).</summary>
public interface IInterruptibleContext
{
    /// <summary>Tries to unblock the given thread; returns true if it became runnable.</summary>
    bool UnblockThread(ThreadContext thread, InterruptionType type);
}

/// <summary>A thread waiting to acquire a resource.</summary>
public sealed class LockWaiter
{
    public bool CanInterrupt { get; }
    public ThreadContext Thread { get; }

    public LockWaiter(bool canInterrupt, ThreadContext thread)
    {
        CanInterrupt = canInterrupt;
        Thread = thread;
    }
}
