using System.Runtime.CompilerServices;
using Fray.Core;
using Fray.Core.Operations;

namespace Fray.Interception;

/// <summary>
/// Replacements for the task-parallel subset of <see cref="Task"/> and for
/// <see cref="TaskCompletionSource"/>, used by the IL rewriter. Controlled
/// <c>Task.Run</c> bodies execute on dedicated Fray-controlled carrier
/// threads while user code keeps holding a real <see cref="Task"/> object;
/// <c>Wait</c>/<c>Result</c>/<c>WaitAll</c> become model joins. Tasks of
/// rewritten <c>async</c> methods participate through promise entries
/// (see <see cref="ControlledAsync"/>).
/// </summary>
public static class ControlledTask
{
    internal sealed class Entry
    {
        public volatile bool Completed;
        public Exception? Exception;

        // Set for tasks created by Delay: the completion deadline instead of
        // a carrier thread.
        public long DelayDeadline = -1;
        public bool IsDelay => DelayDeadline >= 0;
    }

    private static readonly ConditionalWeakTable<Task, Entry> Entries = new();

    /// <summary>Registers a promise entry: a task completed by an explicit signal
    /// (async-method builder, TaskCompletionSource, WhenAll) rather than a carrier.</summary>
    internal static Entry EnsurePromiseEntry(Task task) => Entries.GetValue(task, _ => new Entry());

    internal static bool IsMappedDelay(Task task) =>
        Entries.TryGetValue(task, out var entry) && entry.IsDelay;

    /// <summary>
    /// Completes a promise entry and wakes its model waiters, mirroring the
    /// completion protocol of a finishing controlled thread.
    /// </summary>
    internal static void CompletePromise(RunContext runContext, Task task, Exception? exception)
    {
        var entry = EnsurePromiseEntry(task);
        if (entry.Completed)
        {
            return;
        }
        runContext.MonitorEnter(task, shouldRetry: true);
        entry.Exception ??= exception;
        entry.Completed = true;
        runContext.ObjectPulse(task, all: true);
        runContext.MonitorExit(task);
    }

    /// <summary>Model-joins a task without surfacing its fault (used before
    /// resuming an async state machine, which observes faults via GetResult).</summary>
    internal static void JoinSilently(RunContext runContext, Task task)
    {
        if (Entries.TryGetValue(task, out var entry))
        {
            if (entry.IsDelay)
            {
                DelayJoin(runContext, entry);
            }
            else
            {
                ControlledThreadLifecycle.Join(runContext, task, () => entry.Completed, BlockedOperation.NotTimed);
            }
            return;
        }
        if (task.IsCompleted)
        {
            return;
        }
        throw NotControlled();
    }

    private static NotSupportedException NotControlled() => new(
        "Fray: cannot wait for a Task that was not created through a controlled API " +
        "(Task.Run/Task.Delay/TaskCompletionSource/async methods in rewritten code). " +
        "Uncontrolled task continuations are not supported.");

    public static Task Run(Action action)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return Task.Run(action);
        }
        var completion = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var entry = new Entry();
        Entries.Add(completion.Task, entry);
        ControlledCarrier.Start(runContext, completion.Task, () =>
        {
            try
            {
                action();
            }
            catch (TargetTerminateException)
            {
                throw;
            }
            catch (Exception e)
            {
                // Task semantics: the exception is observed at Wait/Result.
                entry.Exception = e;
            }
        }, () =>
        {
            // Real state first so the fast path (real IsCompleted) never runs
            // ahead of the model.
            if (entry.Exception != null)
            {
                completion.TrySetException(entry.Exception);
            }
            else
            {
                completion.TrySetResult();
            }
            entry.Completed = true;
        }, "task-worker");
        return completion.Task;
    }

    public static Task<TResult> Run<TResult>(Func<TResult> function)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return Task.Run(function);
        }
        var completion = new TaskCompletionSource<TResult>(TaskCreationOptions.RunContinuationsAsynchronously);
        var entry = new Entry();
        Entries.Add(completion.Task, entry);
        TResult result = default!;
        ControlledCarrier.Start(runContext, completion.Task, () =>
        {
            try
            {
                result = function();
            }
            catch (TargetTerminateException)
            {
                throw;
            }
            catch (Exception e)
            {
                entry.Exception = e;
            }
        }, () =>
        {
            if (entry.Exception != null)
            {
                completion.TrySetException(entry.Exception);
            }
            else
            {
                completion.TrySetResult(result);
            }
            entry.Completed = true;
        }, "task-worker");
        return completion.Task;
    }

    public static Task Delay(int millisecondsDelay)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return Task.Delay(millisecondsDelay);
        }
        var completion = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var entry = new Entry
        {
            DelayDeadline = Environment.TickCount64 + Math.Max(0, millisecondsDelay),
        };
        Entries.Add(completion.Task, entry);
        return completion.Task;
    }

    public static Task Delay(TimeSpan delay) => Delay((int)delay.TotalMilliseconds);

    public static void Wait(Task task)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            task.Wait();
            return;
        }
        if (!Entries.TryGetValue(task, out var entry))
        {
            RequireCompleted(task);
            task.Wait(); // Returns immediately; throws if the task faulted.
            return;
        }
        WaitMapped(runContext, task, entry, BlockedOperation.NotTimed);
    }

    public static bool Wait(Task task, int millisecondsTimeout)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return task.Wait(millisecondsTimeout);
        }
        if (!Entries.TryGetValue(task, out var entry))
        {
            RequireCompleted(task);
            return task.Wait(millisecondsTimeout);
        }
        return WaitMapped(runContext, task, entry, FrayMonitor.TimeoutToDeadline(millisecondsTimeout));
    }

    /// <summary>Waiting on an uncontrolled, unfinished task would stall the whole run.</summary>
    private static void RequireCompleted(Task task)
    {
        if (!task.IsCompleted)
        {
            throw NotControlled();
        }
    }

    private static bool WaitMapped(RunContext runContext, Task task, Entry entry, long blockedUntil)
    {
        if (entry.IsDelay)
        {
            DelayJoin(runContext, entry);
            return true;
        }
        var completed = ControlledThreadLifecycle.Join(runContext, task, () => entry.Completed, blockedUntil);
        if (completed && entry.Exception != null)
        {
            throw new AggregateException(entry.Exception);
        }
        return completed;
    }

    private static void DelayJoin(RunContext runContext, Entry entry)
    {
        if (!entry.Completed)
        {
            var remaining = entry.DelayDeadline - Environment.TickCount64;
            if (remaining > 0)
            {
                runContext.ThreadSleep(remaining);
            }
            entry.Completed = true;
        }
    }

    public static TResult Result<TResult>(Task<TResult> task)
    {
        // The completion source is resolved before the join wakes us up, so
        // accessing Result after the controlled wait never blocks.
        Wait(task);
        return task.Result;
    }

    public static void WaitAll(params Task[] tasks)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Task.WaitAll(tasks);
            return;
        }
        var exceptions = new List<Exception>();
        foreach (var task in tasks)
        {
            try
            {
                Wait(task);
            }
            catch (AggregateException e)
            {
                exceptions.AddRange(e.InnerExceptions);
            }
        }
        if (exceptions.Count > 0)
        {
            throw new AggregateException(exceptions);
        }
    }

    public static Task WhenAll(IEnumerable<Task> tasks) => WhenAll(tasks.ToArray());

    public static Task WhenAll(params Task[] tasks)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return Task.WhenAll(tasks);
        }
        var completion = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var entry = EnsurePromiseEntry(completion.Task);
        var children = (Task[])tasks.Clone();
        ControlledCarrier.Start(runContext, new object(), () =>
        {
            var exceptions = new List<Exception>();
            foreach (var child in children)
            {
                JoinSilently(runContext, child);
                if (Entries.TryGetValue(child, out var childEntry) && childEntry.Exception != null)
                {
                    exceptions.Add(childEntry.Exception);
                }
                else if (child.IsFaulted && child.Exception != null)
                {
                    exceptions.AddRange(child.Exception.InnerExceptions);
                }
            }
            if (exceptions.Count > 0)
            {
                completion.TrySetException(exceptions);
            }
            else
            {
                completion.TrySetResult();
            }
            CompletePromise(runContext, completion.Task, exceptions.Count > 0 ? exceptions[0] : null);
        }, () => { }, "whenall-worker");
        return completion.Task;
    }

    public static bool IsCompleted(Task task)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null || !Entries.TryGetValue(task, out var entry))
        {
            return task.IsCompleted;
        }
        if (entry.IsDelay)
        {
            if (!entry.Completed && Environment.TickCount64 >= entry.DelayDeadline)
            {
                entry.Completed = true;
            }
        }
        // A scheduling point: spin-waiting on IsCompleted must let the
        // scheduler run other threads.
        runContext.Yield();
        return entry.Completed;
    }

    // -----------------------------------------------------------------
    // TaskCompletionSource
    // -----------------------------------------------------------------

    public static Task GetTask(TaskCompletionSource source)
    {
        if (FrayRuntime.ControlledContext() != null)
        {
            EnsurePromiseEntry(source.Task);
        }
        return source.Task;
    }

    public static Task<TResult> GetTask<TResult>(TaskCompletionSource<TResult> source)
    {
        if (FrayRuntime.ControlledContext() != null)
        {
            EnsurePromiseEntry(source.Task);
        }
        return source.Task;
    }

    public static void SetResult(TaskCompletionSource source)
    {
        var runContext = FrayRuntime.ControlledContext();
        source.SetResult();
        if (runContext != null)
        {
            CompletePromise(runContext, source.Task, null);
        }
    }

    public static bool TrySetResult(TaskCompletionSource source)
    {
        var runContext = FrayRuntime.ControlledContext();
        var set = source.TrySetResult();
        if (set && runContext != null)
        {
            CompletePromise(runContext, source.Task, null);
        }
        return set;
    }

    public static void SetResult<TResult>(TaskCompletionSource<TResult> source, TResult result)
    {
        var runContext = FrayRuntime.ControlledContext();
        source.SetResult(result);
        if (runContext != null)
        {
            CompletePromise(runContext, source.Task, null);
        }
    }

    public static bool TrySetResult<TResult>(TaskCompletionSource<TResult> source, TResult result)
    {
        var runContext = FrayRuntime.ControlledContext();
        var set = source.TrySetResult(result);
        if (set && runContext != null)
        {
            CompletePromise(runContext, source.Task, null);
        }
        return set;
    }

    public static void SetException(TaskCompletionSource source, Exception exception)
    {
        var runContext = FrayRuntime.ControlledContext();
        source.SetException(exception);
        if (runContext != null)
        {
            CompletePromise(runContext, source.Task, exception);
        }
    }

    public static bool TrySetException(TaskCompletionSource source, Exception exception)
    {
        var runContext = FrayRuntime.ControlledContext();
        var set = source.TrySetException(exception);
        if (set && runContext != null)
        {
            CompletePromise(runContext, source.Task, exception);
        }
        return set;
    }

    public static void SetException<TResult>(TaskCompletionSource<TResult> source, Exception exception)
    {
        var runContext = FrayRuntime.ControlledContext();
        source.SetException(exception);
        if (runContext != null)
        {
            CompletePromise(runContext, source.Task, exception);
        }
    }

    public static bool TrySetException<TResult>(TaskCompletionSource<TResult> source, Exception exception)
    {
        var runContext = FrayRuntime.ControlledContext();
        var set = source.TrySetException(exception);
        if (set && runContext != null)
        {
            CompletePromise(runContext, source.Task, exception);
        }
        return set;
    }
}
