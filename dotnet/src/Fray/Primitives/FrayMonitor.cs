using Fray.Core.Operations;

namespace Fray;

/// <summary>
/// Controlled counterpart of <see cref="Monitor"/>: lock/unlock, wait, and
/// pulse on arbitrary objects. Under Fray, ownership and wait sets are modeled
/// by the engine; outside of Fray it delegates to <see cref="Monitor"/>.
/// </summary>
public static class FrayMonitor
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

    public static bool Wait(object obj) => Wait(obj, Timeout.Infinite);

    public static bool Wait(object obj, int millisecondsTimeout)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return Monitor.Wait(obj, millisecondsTimeout);
        }
        return runContext.ObjectWait(obj, TimeoutToDeadline(millisecondsTimeout), canInterrupt: true);
    }

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

    internal static long TimeoutToDeadline(int millisecondsTimeout) =>
        millisecondsTimeout < 0 ? BlockedOperation.NotTimed : Environment.TickCount64 + millisecondsTimeout;
}
