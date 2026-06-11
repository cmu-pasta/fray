using Fray.Core;
using Xunit;

namespace Fray.Tests;

public class DeadlockTests
{
    [Fact]
    public void FindsLockOrderInversionDeadlock()
    {
        var result = FrayTestRunner.Run(() =>
        {
            var lockA = new FrayLock();
            var lockB = new FrayLock();

            var t1 = FrayThread.StartNew(() =>
            {
                lockA.Lock();
                try
                {
                    lockB.Lock();
                    lockB.Unlock();
                }
                finally
                {
                    lockA.Unlock();
                }
            });
            var t2 = FrayThread.StartNew(() =>
            {
                lockB.Lock();
                try
                {
                    lockA.Lock();
                    lockA.Unlock();
                }
                finally
                {
                    lockB.Unlock();
                }
            });
            t1.Join();
            t2.Join();
        }, new FrayConfiguration { Iterations = 1000, Seed = 3 });

        Assert.NotNull(result.BugFound);
        Assert.IsType<DeadlockException>(result.BugFound);
    }

    [Fact]
    public void OrderedLockAcquisitionHasNoDeadlock()
    {
        var result = FrayTestRunner.Run(() =>
        {
            var lockA = new FrayLock();
            var lockB = new FrayLock();

            void Ordered()
            {
                lockA.Lock();
                try
                {
                    lockB.Lock();
                    lockB.Unlock();
                }
                finally
                {
                    lockA.Unlock();
                }
            }

            var t1 = FrayThread.StartNew(Ordered);
            var t2 = FrayThread.StartNew(Ordered);
            t1.Join();
            t2.Join();
        }, new FrayConfiguration { Iterations = 200, Seed = 3 });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }

    [Fact]
    public void FindsDeadlockFromMissingSignal()
    {
        // The worker can check the flag before the main thread sets it and
        // then wait forever: a lost-wakeup deadlock.
        var result = FrayTestRunner.Run(() =>
        {
            var gate = new object();
            var ready = new FrayShared<bool>(false);

            var waiter = FrayThread.StartNew(() =>
            {
                FrayMonitor.Enter(gate);
                try
                {
                    if (!ready.Value)
                    {
                        FrayMonitor.Wait(gate);
                    }
                }
                finally
                {
                    FrayMonitor.Exit(gate);
                }
            });

            // Bug: the flag is set without holding the lock and without pulsing.
            ready.Value = true;
            waiter.Join();
        }, new FrayConfiguration
        {
            Iterations = 200,
            Seed = 5,
            // Disable spurious wakeups so the lost wakeup reliably deadlocks.
            AllowSpuriousWakeups = false,
        });

        Assert.NotNull(result.BugFound);
        Assert.IsType<DeadlockException>(result.BugFound);
    }
}
