using Fray.Core;

namespace Fray.Interception;

/// <summary>
/// Spawns a controlled carrier thread: the execution vehicle for
/// <see cref="Task"/> bodies and resumed async-method continuations.
/// </summary>
internal static class ControlledCarrier
{
    internal static void Start(RunContext runContext, object joinHandle, Action body, Action markCompleted, string name)
    {
        var started = new ManualResetEventSlim(false);
        ThreadContext? context = null;
        var thread = new Thread(() => ControlledThreadLifecycle.RunControlledBody(
            runContext, context!, joinHandle, started, body, markCompleted))
        {
            IsBackground = true,
            Name = name,
        };
        context = runContext.ThreadCreate(thread, name);
        thread.Start();
        // Rendezvous: the carrier registered itself and parked.
        started.Wait();
    }
}
