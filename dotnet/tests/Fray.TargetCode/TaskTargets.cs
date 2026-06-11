namespace Fray.TargetCode;

/// <summary>
/// Plain task-parallel code (no Fray reference): <see cref="Task.Run(Action)"/>,
/// <c>Wait</c>/<c>Result</c>/<c>WaitAll</c>, spinning on <c>IsCompleted</c>.
/// </summary>
public static class TaskTargets
{
    private sealed class Counter
    {
        private int _value;

        public int Value => _value;

        public void Increment()
        {
            var tmp = _value;
            _value = tmp + 1;
        }
    }

    public static void TaskLostUpdate()
    {
        var counter = new Counter();
        var t1 = Task.Run(counter.Increment);
        var t2 = Task.Run(counter.Increment);
        Task.WaitAll(t1, t2);
        if (counter.Value != 2)
        {
            throw new InvalidOperationException($"Lost update: counter is {counter.Value}, expected 2.");
        }
    }

    public static void TaskLockedUpdate()
    {
        var counter = new Counter();
        var gate = new object();
        var t1 = Task.Run(() =>
        {
            lock (gate)
            {
                counter.Increment();
            }
        });
        var t2 = Task.Run(() =>
        {
            lock (gate)
            {
                counter.Increment();
            }
        });
        t1.Wait();
        t2.Wait();
        if (counter.Value != 2)
        {
            throw new InvalidOperationException($"Locked task update went wrong: counter is {counter.Value}.");
        }
    }

    public static void TaskResults()
    {
        var t1 = Task.Run(() => 20);
        var t2 = Task.Run(() => 22);
        var sum = t1.Result + t2.Result;
        if (sum != 42)
        {
            throw new InvalidOperationException($"Task results went wrong: {sum}.");
        }
    }

    public static void TaskFaultObservation()
    {
        // Explicitly an Action: a throw-only lambda would otherwise bind to
        // the Run(Func<Task>) overload (async tasks stay uncontrolled).
        Action body = () => throw new InvalidOperationException("boom");
        var t = Task.Run(body);
        try
        {
            t.Wait();
        }
        catch (AggregateException e) when (e.InnerException?.Message == "boom")
        {
            return;
        }
        throw new InvalidOperationException("Expected the task fault to surface from Wait.");
    }

    public static void TaskSpinOnIsCompleted()
    {
        var done = false;
        var t = Task.Run(() => { done = true; });
        while (!t.IsCompleted)
        {
        }
        if (!done)
        {
            throw new InvalidOperationException("IsCompleted was true before the task body ran.");
        }
    }

    public static void TaskDelayCompletes()
    {
        var t = Task.Delay(50);
        t.Wait();
    }
}
