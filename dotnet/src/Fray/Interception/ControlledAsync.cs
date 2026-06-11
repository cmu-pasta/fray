using System.Collections.Concurrent;
using System.Reflection;
using System.Runtime.CompilerServices;
using Fray.Core;

namespace Fray.Interception;

/// <summary>
/// Replacements for the compiler-generated async machinery
/// (<see cref="AsyncTaskMethodBuilder"/>, <see cref="TaskAwaiter"/>), used by
/// the IL rewriter. The real builder and awaiter structs are kept — only
/// their key calls are redirected, which keeps every type signature in
/// rewritten code intact.
///
/// Model: <c>MoveNext</c> runs inline up to the first suspension, exactly as
/// in real .NET. At <c>AwaitUnsafeOnCompleted</c> the continuation becomes a
/// controlled carrier thread that model-joins the awaited task and then
/// resumes the state machine, so every async method is a schedulable unit of
/// the exploration. Builder tasks are promise entries completed by
/// <c>SetResult</c>/<c>SetException</c>.
/// </summary>
public static class ControlledAsync
{
    // -----------------------------------------------------------------
    // AsyncTaskMethodBuilder (async Task methods)
    // -----------------------------------------------------------------

    public static Task GetTask(ref AsyncTaskMethodBuilder builder)
    {
        var task = builder.Task;
        if (FrayRuntime.ControlledContext() != null)
        {
            ControlledTask.EnsurePromiseEntry(task);
        }
        return task;
    }

    public static void SetResult(ref AsyncTaskMethodBuilder builder)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            builder.SetResult();
            return;
        }
        var task = builder.Task;
        // Real state first so the fast path (real IsCompleted) never runs
        // ahead of the model.
        builder.SetResult();
        ControlledTask.CompletePromise(runContext, task, null);
    }

    public static void SetException(ref AsyncTaskMethodBuilder builder, Exception exception)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            builder.SetException(exception);
            return;
        }
        var task = builder.Task;
        builder.SetException(exception);
        ControlledTask.CompletePromise(runContext, task, exception);
    }

    public static void AwaitUnsafeOnCompleted<TAwaiter, TStateMachine>(
        ref AsyncTaskMethodBuilder builder, ref TAwaiter awaiter, ref TStateMachine stateMachine)
        where TAwaiter : ICriticalNotifyCompletion
        where TStateMachine : IAsyncStateMachine
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            builder.AwaitUnsafeOnCompleted(ref awaiter, ref stateMachine);
            return;
        }
        // Materialize the builder's task in place BEFORE the state machine is
        // copied below: the copy's embedded builder must share the same Task.
        ControlledTask.EnsurePromiseEntry(builder.Task);
        Suspend(runContext, ref awaiter, stateMachine);
    }

    public static void AwaitOnCompleted<TAwaiter, TStateMachine>(
        ref AsyncTaskMethodBuilder builder, ref TAwaiter awaiter, ref TStateMachine stateMachine)
        where TAwaiter : INotifyCompletion
        where TStateMachine : IAsyncStateMachine
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            builder.AwaitOnCompleted(ref awaiter, ref stateMachine);
            return;
        }
        ControlledTask.EnsurePromiseEntry(builder.Task);
        Suspend(runContext, ref awaiter, stateMachine);
    }

    // -----------------------------------------------------------------
    // AsyncTaskMethodBuilder<TResult> (async Task<TResult> methods)
    // -----------------------------------------------------------------

    public static Task<TResult> GetTask<TResult>(ref AsyncTaskMethodBuilder<TResult> builder)
    {
        var task = builder.Task;
        if (FrayRuntime.ControlledContext() != null)
        {
            ControlledTask.EnsurePromiseEntry(task);
        }
        return task;
    }

    public static void SetResult<TResult>(ref AsyncTaskMethodBuilder<TResult> builder, TResult result)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            builder.SetResult(result);
            return;
        }
        var task = builder.Task;
        builder.SetResult(result);
        ControlledTask.CompletePromise(runContext, task, null);
    }

    public static void SetException<TResult>(ref AsyncTaskMethodBuilder<TResult> builder, Exception exception)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            builder.SetException(exception);
            return;
        }
        var task = builder.Task;
        builder.SetException(exception);
        ControlledTask.CompletePromise(runContext, task, exception);
    }

    public static void AwaitUnsafeOnCompleted<TResult, TAwaiter, TStateMachine>(
        ref AsyncTaskMethodBuilder<TResult> builder, ref TAwaiter awaiter, ref TStateMachine stateMachine)
        where TAwaiter : ICriticalNotifyCompletion
        where TStateMachine : IAsyncStateMachine
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            builder.AwaitUnsafeOnCompleted(ref awaiter, ref stateMachine);
            return;
        }
        ControlledTask.EnsurePromiseEntry(builder.Task);
        Suspend(runContext, ref awaiter, stateMachine);
    }

    public static void AwaitOnCompleted<TResult, TAwaiter, TStateMachine>(
        ref AsyncTaskMethodBuilder<TResult> builder, ref TAwaiter awaiter, ref TStateMachine stateMachine)
        where TAwaiter : INotifyCompletion
        where TStateMachine : IAsyncStateMachine
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            builder.AwaitOnCompleted(ref awaiter, ref stateMachine);
            return;
        }
        ControlledTask.EnsurePromiseEntry(builder.Task);
        Suspend(runContext, ref awaiter, stateMachine);
    }

    // -----------------------------------------------------------------
    // Suspension: continuation carriers
    // -----------------------------------------------------------------

    private static void Suspend<TAwaiter, TStateMachine>(RunContext runContext, ref TAwaiter awaiter,
        TStateMachine stateMachine)
        where TStateMachine : IAsyncStateMachine
    {
        var awaited = ExtractAwaitedTask(ref awaiter);
        var isYield = typeof(TAwaiter) == typeof(YieldAwaitable.YieldAwaiter);
        // The state machine is captured by value (structs in release builds):
        // the abandoned original returned right after storing its state, so
        // the copy carries everything needed to resume — the same effect as
        // the real builder boxing it once.
        var continuation = stateMachine;
        ControlledCarrier.Start(runContext, new object(), () =>
        {
            if (awaited != null)
            {
                // Faults are not surfaced here: the resumed state machine
                // observes them through the awaiter's GetResult.
                ControlledTask.JoinSilently(runContext, awaited);
            }
            else if (!isYield)
            {
                throw new NotSupportedException(
                    $"Fray: await on an unrecognized awaiter type {typeof(TAwaiter).Name} is not controlled.");
            }
            continuation.MoveNext();
        }, () => { }, "async-continuation");
    }

    private static readonly ConcurrentDictionary<Type, FieldInfo?> AwaitedTaskFields = new();

    /// <summary>Pulls the awaited <see cref="Task"/> out of a (Configured)TaskAwaiter.</summary>
    private static Task? ExtractAwaitedTask<TAwaiter>(ref TAwaiter awaiter)
    {
        var field = AwaitedTaskFields.GetOrAdd(typeof(TAwaiter), type =>
            type.GetFields(BindingFlags.Instance | BindingFlags.NonPublic | BindingFlags.Public)
                .FirstOrDefault(f => typeof(Task).IsAssignableFrom(f.FieldType)));
        if (field == null)
        {
            return null;
        }
        object boxed = awaiter!;
        return (Task?)field.GetValue(boxed);
    }

    // -----------------------------------------------------------------
    // TaskAwaiter
    // -----------------------------------------------------------------

    public static void GetResult(ref TaskAwaiter awaiter)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext != null)
        {
            // A Delay task's real Task never completes; its model entry does.
            var awaited = ExtractAwaitedTask(ref awaiter);
            if (awaited != null && ControlledTask.IsMappedDelay(awaited))
            {
                return;
            }
        }
        awaiter.GetResult();
    }
}
