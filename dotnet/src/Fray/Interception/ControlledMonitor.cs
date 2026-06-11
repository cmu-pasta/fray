using Fray.Core.Operations;

namespace Fray.Interception;

/// <summary>
/// Drop-in replacements for <see cref="Monitor"/> used by the IL rewriter.
/// The signatures mirror the BCL methods (with instance-style semantics for
/// the C# <c>lock</c> pattern, which compiles to
/// <c>Monitor.Enter(object, ref bool)</c> / <c>Monitor.Exit(object)</c>).
/// </summary>
public static class ControlledMonitor
{
    public static void Enter(object obj)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Monitor.Enter(obj);
            return;
        }
        runContext.MonitorEnter(obj);
    }

    public static void Enter(object obj, ref bool lockTaken)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Monitor.Enter(obj, ref lockTaken);
            return;
        }
        runContext.MonitorEnter(obj);
        // Only set after the model acquisition succeeded, so the caller's
        // finally block skips Exit when the acquisition was aborted.
        lockTaken = true;
    }

    public static void Exit(object obj)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Monitor.Exit(obj);
            return;
        }
        runContext.MonitorExit(obj);
    }

    public static bool TryEnter(object obj)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return Monitor.TryEnter(obj);
        }
        return runContext.LockTryLock(obj, canInterrupt: false, BlockedOperation.NotTimed);
    }

    public static bool TryEnter(object obj, int millisecondsTimeout)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return Monitor.TryEnter(obj, millisecondsTimeout);
        }
        return runContext.LockTryLock(obj, canInterrupt: true, FrayMonitor.TimeoutToDeadline(millisecondsTimeout));
    }

    public static bool TryEnter(object obj, TimeSpan timeout) =>
        TryEnter(obj, (int)timeout.TotalMilliseconds);

    public static void TryEnter(object obj, ref bool lockTaken) =>
        lockTaken = TryEnter(obj);

    public static void TryEnter(object obj, int millisecondsTimeout, ref bool lockTaken) =>
        lockTaken = TryEnter(obj, millisecondsTimeout);

    public static bool Wait(object obj)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return Monitor.Wait(obj);
        }
        return runContext.ObjectWait(obj, BlockedOperation.NotTimed, canInterrupt: true);
    }

    public static bool Wait(object obj, int millisecondsTimeout)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return Monitor.Wait(obj, millisecondsTimeout);
        }
        return runContext.ObjectWait(obj, FrayMonitor.TimeoutToDeadline(millisecondsTimeout), canInterrupt: true);
    }

    public static bool Wait(object obj, TimeSpan timeout) =>
        Wait(obj, (int)timeout.TotalMilliseconds);

    public static void Pulse(object obj)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Monitor.Pulse(obj);
            return;
        }
        runContext.ObjectPulse(obj, all: false);
    }

    public static void PulseAll(object obj)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Monitor.PulseAll(obj);
            return;
        }
        runContext.ObjectPulse(obj, all: true);
    }
}
