namespace Fray.Core;

/// <summary>
/// Thrown into controlled threads to unwind them once the current execution
/// should stop (a bug was found, or the main thread exited and the
/// configuration requests aborting the remaining threads).
///
/// Mirrors <c>org.pastalab.fray.runtime.TargetTerminateException</c>.
/// </summary>
public class TargetTerminateException : Exception
{
    public TargetTerminateException() : base("Fray terminated the execution of this thread.") { }

    public TargetTerminateException(string message) : base(message) { }
}

/// <summary>All controlled threads are blocked and none can make progress.</summary>
public sealed class DeadlockException : TargetTerminateException
{
    public DeadlockException() : base("Deadlock detected: all threads are blocked.") { }
}

/// <summary>The execution exceeded the configured maximum number of scheduling steps.</summary>
public sealed class LivenessException : TargetTerminateException
{
    public LivenessException() : base("Liveness violation: maximum number of scheduled steps exceeded.") { }
}

/// <summary>An internal invariant of the Fray engine was violated.</summary>
public sealed class FrayInternalException : Exception
{
    public FrayInternalException(string message) : base(message) { }
}
