namespace Fray.TargetCode;

/// <summary>
/// Deliberately plain multithreaded code with no reference to Fray: raw
/// <see cref="Thread"/>, <c>lock</c>, <see cref="Monitor"/>, and
/// <see cref="Interlocked"/>. The rewriter tests rewrite this assembly and
/// verify that Fray controls it.
/// </summary>
public static class PlainTargets
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

    public static void LostUpdate()
    {
        var counter = new Counter();
        var t1 = new Thread(counter.Increment);
        var t2 = new Thread(counter.Increment);
        t1.Start();
        t2.Start();
        t1.Join();
        t2.Join();
        if (counter.Value != 2)
        {
            throw new InvalidOperationException($"Lost update: counter is {counter.Value}, expected 2.");
        }
    }

    public static void LockedUpdate()
    {
        var counter = new Counter();
        var gate = new object();

        void Increment()
        {
            lock (gate)
            {
                counter.Increment();
            }
        }

        var t1 = new Thread(Increment);
        var t2 = new Thread(Increment);
        t1.Start();
        t2.Start();
        t1.Join();
        t2.Join();
        if (counter.Value != 2)
        {
            throw new InvalidOperationException($"Locked update went wrong: counter is {counter.Value}.");
        }
    }

    public static void DeadlockProne()
    {
        var a = new object();
        var b = new object();
        var t1 = new Thread(() =>
        {
            lock (a)
            {
                lock (b)
                {
                }
            }
        });
        var t2 = new Thread(() =>
        {
            lock (b)
            {
                lock (a)
                {
                }
            }
        });
        t1.Start();
        t2.Start();
        t1.Join();
        t2.Join();
    }

    public static void WaitAndPulse()
    {
        var gate = new object();
        var queue = new Queue<int>();
        var consumed = new List<int>();
        const int items = 3;

        var producer = new Thread(() =>
        {
            for (var i = 0; i < items; i++)
            {
                lock (gate)
                {
                    while (queue.Count == 1)
                    {
                        Monitor.Wait(gate);
                    }
                    queue.Enqueue(i);
                    Monitor.PulseAll(gate);
                }
            }
        });
        var consumer = new Thread(() =>
        {
            for (var i = 0; i < items; i++)
            {
                lock (gate)
                {
                    while (queue.Count == 0)
                    {
                        Monitor.Wait(gate);
                    }
                    consumed.Add(queue.Dequeue());
                    Monitor.PulseAll(gate);
                }
            }
        });

        producer.Start();
        consumer.Start();
        producer.Join();
        consumer.Join();
        if (!consumed.SequenceEqual(new[] { 0, 1, 2 }))
        {
            throw new InvalidOperationException($"Consumed out of order: {string.Join(",", consumed)}");
        }
    }

    private sealed class AtomicHolder
    {
        public int Value;
    }

    public static void InterlockedUpdate()
    {
        var holder = new AtomicHolder();
        var t1 = new Thread(() => Interlocked.Increment(ref holder.Value));
        var t2 = new Thread(() => Interlocked.Increment(ref holder.Value));
        t1.Start();
        t2.Start();
        t1.Join();
        t2.Join();
        if (holder.Value != 2)
        {
            throw new InvalidOperationException($"Interlocked update went wrong: {holder.Value}.");
        }
    }
}
