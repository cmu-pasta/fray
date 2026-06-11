using System.Runtime.CompilerServices;
using Fray.Core;
using Fray.Core.Operations;

namespace Fray.Interception;

/// <summary>
/// Replacements for the task-parallel subset of <see cref="Task"/>, used by
/// the IL rewriter. Controlled <c>Task.Run</c> bodies execute on dedicated
/// Fray-controlled carrier threads while user code keeps holding a real
/// <see cref="Task"/> object; <c>Wait</c>/<c>Result</c>/<c>WaitAll</c> become
/// model joins. <c>async</c>/<c>await</c> is not supported: waiting on a task
/// that was not created through a controlled API fails fast instead of
/// blocking the run.
/// </summary>
public static class ControlledTask
{
    private sealed class Entry
    {
        public RunContext? RunContext;
        public ThreadContext? Context;
        public readonly ManualResetEventSlim Started = new(false);
        public volatile bool Completed;
        public Exception? Exception;

        // Set for tasks created by Delay: the completion deadline instead of
        // a carrier thread.
        public long DelayDeadline = -1;
        public bool IsDelay => DelayDeadline >= 0;
    }

    private static readonly ConditionalWeakTable<Task, Entry> Entries = new();

    public static Task Run(Action action)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            return Task.Run(action);
        }
        var completion = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        var entry = new Entry();
        StartCarrier(runContext, entry, completion.Task, () =>
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
            entry.Completed = true;
            if (entry.Exception != null)
            {
                completion.TrySetException(entry.Exception);
            }
            else
            {
                completion.TrySetResult();
            }
        });
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
        TResult result = default!;
        StartCarrier(runContext, entry, completion.Task, () =>
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
            entry.Completed = true;
            if (entry.Exception != null)
            {
                completion.TrySetException(entry.Exception);
            }
            else
            {
                completion.TrySetResult(result);
            }
        });
        return completion.Task;
    }

    private static void StartCarrier(RunContext runContext, Entry entry, Task task, Action body, Action markCompleted)
    {
        Entries.Add(task, entry);
        var thread = new Thread(() =>
            ControlledThreadLifecycle.RunControlledBody(entry.RunContext!, entry.Context!, task,
                entry.Started, body, markCompleted))
        {
            IsBackground = true,
            Name = "task-worker",
        };
        entry.RunContext = runContext;
        entry.Context = runContext.ThreadCreate(thread, thread.Name!);
        thread.Start();
        entry.Started.Wait();
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
            RunContext = runContext,
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
            throw new NotSupportedException(
                "Fray: cannot wait for a Task that was not created through a controlled API " +
                "(Task.Run/Task.Delay in rewritten code). async/await and task continuations " +
                "are not controlled yet.");
        }
    }

    private static bool WaitMapped(RunContext runContext, Task task, Entry entry, long blockedUntil)
    {
        if (entry.IsDelay)
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
            return true;
        }
        var completed = ControlledThreadLifecycle.Join(runContext, task, () => entry.Completed, blockedUntil);
        if (completed && entry.Exception != null)
        {
            throw new AggregateException(entry.Exception);
        }
        return completed;
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
}
