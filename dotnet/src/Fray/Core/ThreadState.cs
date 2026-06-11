namespace Fray.Core;

/// <summary>State of a controlled thread, as tracked by the Fray engine.</summary>
public enum FrayThreadState
{
    /// <summary>The thread object exists but has not started running yet.</summary>
    Created,

    /// <summary>The thread is able to run and waits to be picked by the scheduler.</summary>
    Runnable,

    /// <summary>The thread is the (single) currently executing thread.</summary>
    Running,

    /// <summary>The thread is blocked on a controlled primitive.</summary>
    Blocked,

    /// <summary>The main thread finished the test body and waits for remaining threads.</summary>
    MainExiting,

    /// <summary>The thread finished execution.</summary>
    Completed,
}

/// <summary>The kind of memory access performed by a racing operation.</summary>
public enum MemoryOpType
{
    MemoryRead,
    MemoryWrite,
}

/// <summary>Why a blocked thread is being unblocked.</summary>
public enum InterruptionType
{
    Timeout,
    Interrupt,
    Force,
    ResourceAvailable,
}
