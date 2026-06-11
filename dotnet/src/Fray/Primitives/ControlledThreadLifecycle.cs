using Fray.Core;
using Fray.Core.Operations;

namespace Fray;

/// <summary>
/// The lifecycle protocol shared by every kind of controlled thread
/// (<see cref="FrayThread"/> and rewritten <see cref="Thread"/> instances):
/// registration handshake, body execution with bug reporting, completion, and
/// Java-style join on the thread's handle monitor.
/// </summary>
internal static class ControlledThreadLifecycle
{
    internal static void RunControlledBody(RunContext runContext, ThreadContext context, object joinHandle,
        ManualResetEventSlim started, Action body, Action markCompleted)
    {
        runContext.ThreadRun(context, started);
        try
        {
            body();
        }
        catch (TargetTerminateException)
        {
            // The execution is winding down (bug found or main exited).
        }
        catch (Exception e)
        {
            if (!runContext.Config.IgnoreUnhandledExceptions)
            {
                runContext.ReportError(e);
            }
        }
        finally
        {
            markCompleted();
            runContext.ThreadCompleted(joinHandle, context);
        }
    }

    /// <summary>Waits on the handle's monitor until <paramref name="completed"/>; Java's join.</summary>
    internal static bool Join(RunContext runContext, object joinHandle, Func<bool> completed, long blockedUntil)
    {
        runContext.MonitorEnter(joinHandle);
        try
        {
            while (!completed())
            {
                var noTimeout = runContext.ObjectWait(joinHandle, blockedUntil, canInterrupt: true);
                // A forced wakeup during wind-down means the awaited completion
                // will never arrive: unwind instead of re-waiting forever.
                if (!completed() && runContext.IsWindingDown)
                {
                    throw new TargetTerminateException();
                }
                if (!noTimeout && blockedUntil != BlockedOperation.NotTimed)
                {
                    return completed();
                }
            }
            return true;
        }
        finally
        {
            runContext.MonitorExit(joinHandle);
        }
    }
}
