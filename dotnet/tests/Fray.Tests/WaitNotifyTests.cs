using Xunit;

namespace Fray.Tests;

public class WaitNotifyTests
{
    [Fact]
    public void ProducerConsumerWithProperWaitLoopsIsCorrect()
    {
        var result = FrayTestRunner.Run(() =>
        {
            var gate = new object();
            var queue = new Queue<int>();
            const int capacity = 1;
            const int items = 3;
            var consumed = new List<int>();

            var producer = FrayThread.StartNew(() =>
            {
                for (var i = 0; i < items; i++)
                {
                    FrayMonitor.Enter(gate);
                    try
                    {
                        while (queue.Count == capacity)
                        {
                            FrayMonitor.Wait(gate);
                        }
                        queue.Enqueue(i);
                        FrayMonitor.PulseAll(gate);
                    }
                    finally
                    {
                        FrayMonitor.Exit(gate);
                    }
                }
            });

            var consumer = FrayThread.StartNew(() =>
            {
                for (var i = 0; i < items; i++)
                {
                    FrayMonitor.Enter(gate);
                    try
                    {
                        while (queue.Count == 0)
                        {
                            FrayMonitor.Wait(gate);
                        }
                        consumed.Add(queue.Dequeue());
                        FrayMonitor.PulseAll(gate);
                    }
                    finally
                    {
                        FrayMonitor.Exit(gate);
                    }
                }
            });

            producer.Join();
            consumer.Join();
            Assert.Equal(new[] { 0, 1, 2 }, consumed);
        }, new FrayConfiguration { Iterations = 150, Seed = 17 });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }

    [Fact]
    public void FindsBugWhenWaitConditionUsesIfInsteadOfWhile()
    {
        // Two consumers + PulseAll: both can be woken for a single item. The
        // `if` check (instead of `while`) lets the second consumer dequeue
        // from an empty queue.
        var result = FrayTestRunner.Run(() =>
        {
            var gate = new object();
            var queue = new Queue<int>();

            void Consume()
            {
                FrayMonitor.Enter(gate);
                try
                {
                    if (queue.Count == 0)
                    {
                        FrayMonitor.Wait(gate);
                    }
                    if (queue.Count == 0)
                    {
                        throw new InvalidOperationException("Dequeue from empty queue.");
                    }
                    queue.Dequeue();
                }
                finally
                {
                    FrayMonitor.Exit(gate);
                }
            }

            var c1 = FrayThread.StartNew(Consume);
            var c2 = FrayThread.StartNew(Consume);
            var producer = FrayThread.StartNew(() =>
            {
                FrayMonitor.Enter(gate);
                try
                {
                    queue.Enqueue(1);
                    FrayMonitor.PulseAll(gate);
                }
                finally
                {
                    FrayMonitor.Exit(gate);
                }
            });

            producer.Join();
            c1.Join();
            c2.Join();
        }, new FrayConfiguration { Iterations = 500, Seed = 23 });

        Assert.NotNull(result.BugFound);
        // Either the empty dequeue is caught or the second consumer waits
        // forever (deadlock) — both are real bugs of this code.
    }

    [Fact]
    public void ConditionVariableHandoffWorks()
    {
        var result = FrayTestRunner.Run(() =>
        {
            var mutex = new FrayLock();
            var nonEmpty = mutex.NewCondition();
            var slot = new FrayShared<int>(0);
            var hasValue = new FrayShared<bool>(false);

            var consumer = FrayThread.StartNew(() =>
            {
                mutex.Lock();
                try
                {
                    while (!hasValue.Value)
                    {
                        nonEmpty.Await();
                    }
                    Assert.Equal(42, slot.Value);
                }
                finally
                {
                    mutex.Unlock();
                }
            });

            mutex.Lock();
            try
            {
                slot.Value = 42;
                hasValue.Value = true;
                nonEmpty.SignalAll();
            }
            finally
            {
                mutex.Unlock();
            }
            consumer.Join();
        }, new FrayConfiguration { Iterations = 150, Seed = 29 });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }

    [Fact]
    public void WaitWithoutHoldingMonitorIsReported()
    {
        var result = FrayTestRunner.Run(() =>
        {
            var gate = new object();
            FrayMonitor.Wait(gate);
        }, new FrayConfiguration { Iterations = 1, Seed = 1 });

        Assert.NotNull(result.BugFound);
        Assert.IsType<SynchronizationLockException>(result.BugFound);
    }

    [Fact]
    public void InterruptWakesBlockedThread()
    {
        var result = FrayTestRunner.Run(() =>
        {
            var mutex = new FrayLock();
            var interrupted = new FrayShared<bool>(false);

            mutex.Lock();
            var blocked = FrayThread.StartNew(() =>
            {
                try
                {
                    mutex.LockInterruptibly();
                    mutex.Unlock();
                }
                catch (ThreadInterruptedException)
                {
                    interrupted.Value = true;
                }
            });

            blocked.Interrupt();
            mutex.Unlock();
            blocked.Join();
            Assert.True(interrupted.Value);
        }, new FrayConfiguration { Iterations = 150, Seed = 31 });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }
}
