using System.Runtime.CompilerServices;
using Fray.Core;
using Fray.Core.Operations;

namespace Fray.Interception;

/// <summary>
/// Replacements for <see cref="Thread"/> constructors and lifecycle methods,
/// used by the IL rewriter. A rewritten <c>new Thread(body)</c> becomes
/// <see cref="Create(ThreadStart)"/>, which returns a real <see cref="Thread"/>
/// whose body is wrapped with the Fray lifecycle protocol; instance calls like
/// <c>thread.Start()</c> become static calls taking the thread as the first
/// argument. Threads started outside a Fray run behave like plain threads.
/// </summary>
public static class ControlledThread
{
    private sealed class Entry
    {
        public RunContext? RunContext;
        public ThreadContext? Context;
        public readonly ManualResetEventSlim Started = new(false);
        public volatile bool Completed;
    }

    private static readonly ConditionalWeakTable<Thread, Entry> Entries = new();

    public static Thread Create(ThreadStart start)
    {
        var entry = new Entry();
        Thread thread = null!;
        thread = new Thread(() => RunBody(thread, entry, () => start()));
        Entries.Add(thread, entry);
        return thread;
    }

    public static Thread Create(ThreadStart start, int maxStackSize)
    {
        var entry = new Entry();
        Thread thread = null!;
        thread = new Thread(() => RunBody(thread, entry, () => start()), maxStackSize);
        Entries.Add(thread, entry);
        return thread;
    }

    public static Thread Create(ParameterizedThreadStart start)
    {
        var entry = new Entry();
        Thread thread = null!;
        thread = new Thread(parameter => RunBody(thread, entry, () => start(parameter)));
        Entries.Add(thread, entry);
        return thread;
    }

    public static Thread Create(ParameterizedThreadStart start, int maxStackSize)
    {
        var entry = new Entry();
        Thread thread = null!;
        thread = new Thread(parameter => RunBody(thread, entry, () => start(parameter)), maxStackSize);
        Entries.Add(thread, entry);
        return thread;
    }

    private static void RunBody(Thread thread, Entry entry, Action body)
    {
        var runContext = entry.RunContext;
        var context = entry.Context;
        if (runContext == null || context == null)
        {
            body();
            return;
        }
        ControlledThreadLifecycle.RunControlledBody(runContext, context, thread, entry.Started, body,
            () => entry.Completed = true);
    }

    public static void Start(Thread thread) => StartImpl(thread, null, hasParameter: false);

    public static void Start(Thread thread, object? parameter) => StartImpl(thread, parameter, hasParameter: true);

    private static void StartImpl(Thread thread, object? parameter, bool hasParameter)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext != null && Entries.TryGetValue(thread, out var entry) && entry.Context == null)
        {
            entry.RunContext = runContext;
            entry.Context = runContext.ThreadCreate(thread, thread.Name ?? "thread");
            if (hasParameter)
            {
                thread.Start(parameter);
            }
            else
            {
                thread.Start();
            }
            // Rendezvous: the child registered itself and parked.
            entry.Started.Wait();
            return;
        }
        if (hasParameter)
        {
            thread.Start(parameter);
        }
        else
        {
            thread.Start();
        }
    }

    public static void Join(Thread thread)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext != null && Entries.TryGetValue(thread, out var entry) &&
            ReferenceEquals(entry.RunContext, runContext))
        {
            ControlledThreadLifecycle.Join(runContext, thread, () => entry.Completed, BlockedOperation.NotTimed);
            return;
        }
        thread.Join();
    }

    public static bool Join(Thread thread, int millisecondsTimeout)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext != null && Entries.TryGetValue(thread, out var entry) &&
            ReferenceEquals(entry.RunContext, runContext))
        {
            return ControlledThreadLifecycle.Join(runContext, thread, () => entry.Completed,
                FrayMonitor.TimeoutToDeadline(millisecondsTimeout));
        }
        return thread.Join(millisecondsTimeout);
    }

    public static void Interrupt(Thread thread)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext != null && Entries.TryGetValue(thread, out var entry) &&
            ReferenceEquals(entry.RunContext, runContext))
        {
            runContext.ThreadInterrupt(entry.Context!);
            return;
        }
        thread.Interrupt();
    }

    public static void Sleep(int millisecondsTimeout)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Thread.Sleep(millisecondsTimeout);
            return;
        }
        runContext.ThreadSleep(millisecondsTimeout);
    }

    public static void Sleep(TimeSpan timeout) => Sleep((int)timeout.TotalMilliseconds);

    public static bool Yield()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return Thread.Yield();
        }
        runContext.Yield();
        return true;
    }
}
