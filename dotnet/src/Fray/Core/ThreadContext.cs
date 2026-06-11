using Fray.Core.Contexts;
using Fray.Core.Operations;

namespace Fray.Core;

/// <summary>
/// Engine-side state of a controlled thread: its scheduler state, pending
/// operation, and the handoff signal used to park/resume the underlying OS
/// thread. Only one controlled thread executes at a time; all others are
/// parked in <see cref="Block"/>.
///
/// Mirrors <c>org.pastalab.fray.core.ThreadContext</c>.
/// </summary>
public sealed class ThreadContext
{
    // Counting handoff signal: an Unblock may arrive just before the thread
    // parks itself, so a plain event is not enough (mirrors core's Sync).
    private readonly SemaphoreSlim _signal = new(0);

    public RunContext RunContext { get; }
    public Thread Thread { get; }
    public string Name { get; }

    /// <summary>Stable, deterministic id of this thread within the run (registration order).</summary>
    public int Index { get; }

    /// <summary>Index of the thread that created this thread, or -1 for the main thread.</summary>
    public int ParentIndex { get; }

    public FrayThreadState State { get; set; } = FrayThreadState.Created;
    public Operation PendingOperation { get; set; }
    public bool InterruptSignaled { get; set; }
    public bool IsExiting { get; set; }
    public HashSet<Acquirable> AcquiredResources { get; } = new();

    internal ThreadContext(RunContext runContext, Thread thread, string name, int index, int parentIndex)
    {
        RunContext = runContext;
        Thread = thread;
        Name = name;
        Index = index;
        ParentIndex = parentIndex;
        PendingOperation = new ThreadStartOperation(parentIndex);
    }

    public bool Schedulable() => State is FrayThreadState.Runnable or FrayThreadState.Running;

    /// <summary>Parks the calling thread until the scheduler picks it again.</summary>
    public void Block() => _signal.Wait();

    /// <summary>Allows the (parked) thread to resume.</summary>
    public void Unblock() => _signal.Release();

    /// <summary>
    /// Throws <see cref="ThreadInterruptedException"/> when an interrupt is
    /// pending, clearing the interrupt flag (Java/.NET interrupt semantics).
    /// </summary>
    public void CheckInterrupt()
    {
        if (InterruptSignaled)
        {
            InterruptSignaled = false;
            throw new ThreadInterruptedException();
        }
    }

    public override string ToString() => $"{Name}(index={Index}, state={State}, op={PendingOperation})";
}
