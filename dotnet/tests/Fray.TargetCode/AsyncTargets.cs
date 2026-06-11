namespace Fray.TargetCode;

/// <summary>
/// Plain <c>async</c>/<c>await</c> code (no Fray reference): async methods,
/// <see cref="Task.Yield"/>, <see cref="Task.Delay(int)"/>,
/// <see cref="TaskCompletionSource{T}"/>, and <see cref="Task.WhenAll(Task[])"/>.
/// </summary>
public static class AsyncTargets
{
    private sealed class Counter
    {
        private int _value;

        public int Value => _value;

        public void Force(int value) => _value = value;

        public void Increment()
        {
            var tmp = _value;
            _value = tmp + 1;
        }
    }

    private static async Task IncrementAcrossAwaitAsync(Counter counter)
    {
        var tmp = counter.Value;
        await Task.Delay(1);
        counter.Force(tmp + 1);
    }

    public static void AsyncLostUpdate()
    {
        var counter = new Counter();
        var t1 = IncrementAcrossAwaitAsync(counter);
        var t2 = IncrementAcrossAwaitAsync(counter);
        t1.Wait();
        t2.Wait();
        if (counter.Value != 2)
        {
            throw new InvalidOperationException($"Lost update: counter is {counter.Value}, expected 2.");
        }
    }

    private static async Task LockedIncrementAsync(Counter counter, object gate)
    {
        await Task.Yield();
        lock (gate)
        {
            counter.Increment();
        }
    }

    public static void AsyncLockedUpdate()
    {
        var counter = new Counter();
        var gate = new object();
        var t1 = LockedIncrementAsync(counter, gate);
        var t2 = LockedIncrementAsync(counter, gate);
        t1.Wait();
        t2.Wait();
        if (counter.Value != 2)
        {
            throw new InvalidOperationException($"Locked async update went wrong: counter is {counter.Value}.");
        }
    }

    private static async Task<int> AddAsync()
    {
        var a = Task.Run(() => 20);
        var b = Task.Run(() => 22);
        return await a + await b;
    }

    public static void AsyncTaskComposition()
    {
        var sum = AddAsync().Result;
        if (sum != 42)
        {
            throw new InvalidOperationException($"Async composition went wrong: {sum}.");
        }
    }

    public static void AsyncTcsHandshake()
    {
        var tcs = new TaskCompletionSource<int>();

        async Task<int> WaitForSignalAsync() => await tcs.Task;

        var waiter = WaitForSignalAsync();
        tcs.SetResult(7);
        var value = waiter.Result;
        if (value != 7)
        {
            throw new InvalidOperationException($"Handshake delivered {value}, expected 7.");
        }
    }

    private static async Task BoomAsync()
    {
        await Task.Yield();
        throw new InvalidOperationException("async-boom");
    }

    public static void AsyncFaultObservation()
    {
        var t = BoomAsync();
        try
        {
            t.Wait();
        }
        catch (AggregateException e) when (e.InnerException?.Message == "async-boom")
        {
            return;
        }
        throw new InvalidOperationException("Expected the async fault to surface from Wait.");
    }

    public static void AsyncWhenAll()
    {
        var counter = new Counter();
        var gate = new object();
        var t1 = LockedIncrementAsync(counter, gate);
        var t2 = LockedIncrementAsync(counter, gate);
        Task.WhenAll(t1, t2).Wait();
        if (counter.Value != 2)
        {
            throw new InvalidOperationException($"WhenAll update went wrong: counter is {counter.Value}.");
        }
    }
}
