using Xunit;

namespace Fray.Tests;

public class ConcurrencyBugTests
{
    private static void LostUpdateBody()
    {
        var counter = new FrayShared<int>(0);
        var t1 = FrayThread.StartNew(() => counter.Value = counter.Value + 1);
        var t2 = FrayThread.StartNew(() => counter.Value = counter.Value + 1);
        t1.Join();
        t2.Join();
        if (counter.Value != 2)
        {
            throw new InvalidOperationException($"Lost update: counter is {counter.Value}, expected 2.");
        }
    }

    [Theory]
    [InlineData(SchedulerKind.Random)]
    [InlineData(SchedulerKind.Pct)]
    [InlineData(SchedulerKind.Pos)]
    public void FindsLostUpdateWithoutLock(SchedulerKind scheduler)
    {
        var result = FrayTestRunner.Run(LostUpdateBody, new FrayConfiguration
        {
            Scheduler = scheduler,
            Iterations = 500,
            Seed = 42,
        });

        Assert.NotNull(result.BugFound);
        Assert.IsType<InvalidOperationException>(result.BugFound);
        Assert.Contains("Lost update", result.BugFound!.Message);
    }

    [Fact]
    public void LockProtectedCounterHasNoLostUpdate()
    {
        var result = FrayTestRunner.Run(() =>
        {
            var counter = new FrayShared<int>(0);
            var mutex = new FrayLock();

            void Increment() => mutex.WithLock(() => counter.Value = counter.Value + 1);

            var t1 = FrayThread.StartNew(Increment);
            var t2 = FrayThread.StartNew(Increment);
            t1.Join();
            t2.Join();
            Assert.Equal(2, counter.Value);
        }, new FrayConfiguration { Iterations = 100, Seed = 7 });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }

    [Fact]
    public void MonitorProtectedCounterHasNoLostUpdate()
    {
        var result = FrayTestRunner.Run(() =>
        {
            var counter = new FrayShared<int>(0);
            var gate = new object();

            void Increment()
            {
                FrayMonitor.Enter(gate);
                try
                {
                    counter.Value = counter.Value + 1;
                }
                finally
                {
                    FrayMonitor.Exit(gate);
                }
            }

            var t1 = FrayThread.StartNew(Increment);
            var t2 = FrayThread.StartNew(Increment);
            t1.Join();
            t2.Join();
            Assert.Equal(2, counter.Value);
        }, new FrayConfiguration { Iterations = 100, Seed = 7 });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }

    [Fact]
    public void AtomicIncrementHasNoLostUpdate()
    {
        var result = FrayTestRunner.Run(() =>
        {
            var counter = new FrayAtomicInt32(0);
            var t1 = FrayThread.StartNew(() => counter.IncrementAndGet());
            var t2 = FrayThread.StartNew(() => counter.IncrementAndGet());
            t1.Join();
            t2.Join();
            Assert.Equal(2, counter.Value);
        }, new FrayConfiguration { Iterations = 100, Seed = 7 });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }

    [Fact]
    public void FindsCheckThenActRaceOnCompareAndSet()
    {
        // Both threads observe the same value before either CAS commits; the
        // loser's CAS fails, which this (buggy) code treats as an error.
        var result = FrayTestRunner.Run(() =>
        {
            var slot = new FrayAtomicInt32(0);
            var failures = new FrayShared<int>(0);

            void Claim()
            {
                if (slot.Value == 0 && !slot.CompareAndSet(0, 1))
                {
                    failures.Value = failures.Value + 1;
                }
            }

            var t1 = FrayThread.StartNew(Claim);
            var t2 = FrayThread.StartNew(Claim);
            t1.Join();
            t2.Join();
            if (failures.Value > 0)
            {
                throw new InvalidOperationException("CAS failed after check.");
            }
        }, new FrayConfiguration { Iterations = 500, Seed = 11 });

        Assert.NotNull(result.BugFound);
        Assert.Contains("CAS failed", result.BugFound!.Message);
    }
}
