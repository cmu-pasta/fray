using Xunit;

namespace Fray.Tests;

/// <summary>
/// Outside of a Fray run, the controlled primitives must behave like their
/// regular .NET counterparts so the same code can run uncontrolled.
/// </summary>
public class PassthroughTests
{
    [Fact]
    public void LockAndThreadsWorkWithoutFray()
    {
        Assert.False(FrayRuntime.IsControlled);
        var mutex = new FrayLock();
        var counter = 0;

        void Increment()
        {
            for (var i = 0; i < 1000; i++)
            {
                mutex.WithLock(() => counter++);
            }
        }

        var t1 = new FrayThread(Increment);
        var t2 = new FrayThread(Increment);
        t1.Start();
        t2.Start();
        t1.Join();
        t2.Join();
        Assert.Equal(2000, counter);
    }

    [Fact]
    public void CountdownEventWorksWithoutFray()
    {
        var ready = new FrayCountdownEvent(2);
        var t1 = FrayThread.StartNew(ready.Signal);
        var t2 = FrayThread.StartNew(ready.Signal);
        Assert.True(ready.Wait(10_000));
        t1.Join();
        t2.Join();
        Assert.Equal(0, ready.CurrentCount);
    }

    [Fact]
    public void SemaphoreWorksWithoutFray()
    {
        var semaphore = new FraySemaphore(1);
        semaphore.Acquire();
        Assert.False(semaphore.TryAcquire());
        semaphore.Release();
        Assert.True(semaphore.TryAcquire());
        semaphore.Release();
        Assert.Equal(1, semaphore.AvailablePermits);
    }

    [Fact]
    public void MonitorWaitPulseWorksWithoutFray()
    {
        var gate = new object();
        var done = false;

        var waiter = FrayThread.StartNew(() =>
        {
            FrayMonitor.Enter(gate);
            try
            {
                while (!done)
                {
                    FrayMonitor.Wait(gate, 10_000);
                }
            }
            finally
            {
                FrayMonitor.Exit(gate);
            }
        });

        FrayMonitor.Enter(gate);
        try
        {
            done = true;
            FrayMonitor.PulseAll(gate);
        }
        finally
        {
            FrayMonitor.Exit(gate);
        }
        waiter.Join();
        Assert.True(done);
    }
}
