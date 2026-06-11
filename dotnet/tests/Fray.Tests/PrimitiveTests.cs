using Xunit;

namespace Fray.Tests;

public class PrimitiveTests
{
    [Fact]
    public void BinarySemaphoreActsAsMutex()
    {
        var result = FrayTestRunner.Run(() =>
        {
            var semaphore = new FraySemaphore(1);
            var counter = new FrayShared<int>(0);

            void Increment()
            {
                semaphore.Acquire();
                try
                {
                    counter.Value = counter.Value + 1;
                }
                finally
                {
                    semaphore.Release();
                }
            }

            var t1 = FrayThread.StartNew(Increment);
            var t2 = FrayThread.StartNew(Increment);
            t1.Join();
            t2.Join();
            Assert.Equal(2, counter.Value);
        }, new FrayConfiguration { Iterations = 150, Seed = 37 });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }

    [Fact]
    public void FindsOverParallelismWhenSemaphoreIsTooWide()
    {
        // A semaphore with 2 permits cannot protect a single-occupancy
        // resource: Fray finds the schedule where both threads are inside.
        var result = FrayTestRunner.Run(() =>
        {
            var semaphore = new FraySemaphore(2);
            var inside = new FrayShared<int>(0);

            void Enter()
            {
                semaphore.Acquire();
                try
                {
                    inside.Value = inside.Value + 1;
                    if (inside.Value > 1)
                    {
                        throw new InvalidOperationException("Two threads inside the critical section.");
                    }
                    inside.Value = inside.Value - 1;
                }
                finally
                {
                    semaphore.Release();
                }
            }

            var t1 = FrayThread.StartNew(Enter);
            var t2 = FrayThread.StartNew(Enter);
            t1.Join();
            t2.Join();
        }, new FrayConfiguration { Iterations = 500, Seed = 41 });

        Assert.NotNull(result.BugFound);
        Assert.Contains("Two threads inside", result.BugFound!.Message);
    }

    [Fact]
    public void CountdownEventOrdersPublication()
    {
        var result = FrayTestRunner.Run(() =>
        {
            var ready = new FrayCountdownEvent(2);
            var a = new FrayShared<int>(0);
            var b = new FrayShared<int>(0);

            var t1 = FrayThread.StartNew(() =>
            {
                a.Value = 1;
                ready.Signal();
            });
            var t2 = FrayThread.StartNew(() =>
            {
                b.Value = 2;
                ready.Signal();
            });

            ready.Wait();
            Assert.Equal(1, a.Value);
            Assert.Equal(2, b.Value);
            Assert.Equal(0, ready.CurrentCount);
            t1.Join();
            t2.Join();
        }, new FrayConfiguration { Iterations = 150, Seed = 43 });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }

    [Fact]
    public void ReaderWriterLockExcludesWritersFromReaders()
    {
        var result = FrayTestRunner.Run(() =>
        {
            var rwLock = new FrayReaderWriterLock();
            // Atomic counters: two readers may run concurrently, so plain
            // shared variables would race on the counter itself.
            var readers = new FrayAtomicInt32(0);
            var writers = new FrayAtomicInt32(0);

            void Read()
            {
                rwLock.EnterReadLock();
                try
                {
                    readers.IncrementAndGet();
                    if (writers.Value != 0)
                    {
                        throw new InvalidOperationException("Reader observed an active writer.");
                    }
                    readers.DecrementAndGet();
                }
                finally
                {
                    rwLock.ExitReadLock();
                }
            }

            void Write()
            {
                rwLock.EnterWriteLock();
                try
                {
                    writers.IncrementAndGet();
                    if (readers.Value != 0)
                    {
                        throw new InvalidOperationException("Writer observed active readers.");
                    }
                    writers.DecrementAndGet();
                }
                finally
                {
                    rwLock.ExitWriteLock();
                }
            }

            var r1 = FrayThread.StartNew(Read);
            var r2 = FrayThread.StartNew(Read);
            var w = FrayThread.StartNew(Write);
            r1.Join();
            r2.Join();
            w.Join();
        }, new FrayConfiguration { Iterations = 300, Seed = 47 });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }

    [Fact]
    public void SleepDoesNotConsumeWallClockTime()
    {
        var start = Environment.TickCount64;
        var result = FrayTestRunner.Run(() =>
        {
            var t = FrayThread.StartNew(() => FrayThread.Sleep(60_000));
            t.Join();
        }, new FrayConfiguration { Iterations = 5, Seed = 53 });

        Assert.True(result.BugFound == null, result.ErrorReport);
        // 5 iterations of a 60s sleep finish quickly: timeouts fire as soon as
        // no other thread can run (IgnoreTimedBlock).
        Assert.True(Environment.TickCount64 - start < 30_000, "Controlled sleep took wall-clock time.");
    }
}
