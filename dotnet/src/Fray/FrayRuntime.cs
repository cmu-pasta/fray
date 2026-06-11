using Fray.Core;

namespace Fray;

/// <summary>
/// Ambient access to the active controlled execution. Controlled primitives
/// route through the engine only when (1) a Fray execution is installed and
/// (2) the calling thread is registered with it; otherwise they fall through
/// to the regular .NET primitives, so the same test code also runs normally
/// outside of Fray.
///
/// Plays the role of <c>org.pastalab.fray.runtime.Runtime</c> and its
/// delegate switching.
/// </summary>
public static class FrayRuntime
{
    private static volatile RunContext? _activeContext;

    [ThreadStatic] private static ThreadContext? _currentThreadContext;
    [ThreadStatic] private static int _engineDepth;

    /// <summary>The engine-side context of the calling thread, if registered.</summary>
    public static ThreadContext? CurrentThreadContext
    {
        get => _currentThreadContext;
        internal set => _currentThreadContext = value;
    }

    /// <summary>Whether the calling thread is currently controlled by Fray.</summary>
    public static bool IsControlled => ControlledContext() != null;

    /// <summary>
    /// Returns the active run context when the calling thread is controlled by
    /// it, otherwise null (primitive should use its passthrough path).
    /// </summary>
    internal static RunContext? ControlledContext()
    {
        var active = _activeContext;
        var threadContext = _currentThreadContext;
        if (active == null || threadContext == null || _engineDepth > 0)
        {
            return null;
        }
        return ReferenceEquals(threadContext.RunContext, active) ? active : null;
    }

    internal static void Install(RunContext context) => _activeContext = context;

    internal static void Uninstall() => _activeContext = null;

    /// <summary>Marks engine-internal code so nested primitive uses pass through.</summary>
    internal static EngineScope EnterEngine() => new();

    internal readonly struct EngineScope : IDisposable
    {
        public EngineScope() => _engineDepth++;

        public void Dispose() => _engineDepth--;
    }
}
