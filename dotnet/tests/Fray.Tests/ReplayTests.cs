using Xunit;

namespace Fray.Tests;

public class ReplayTests
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

    [Fact]
    public void SavedFailingScheduleReplaysToTheSameBug()
    {
        var reportDirectory = Path.Combine(Path.GetTempPath(), $"fray-report-{Guid.NewGuid():N}");
        try
        {
            var exploration = FrayTestRunner.Run(LostUpdateBody, new FrayConfiguration
            {
                Iterations = 500,
                Seed = 1234,
                ReportDirectory = reportDirectory,
            });
            Assert.NotNull(exploration.BugFound);
            Assert.NotNull(exploration.ReportPath);
            Assert.True(File.Exists(Path.Combine(exploration.ReportPath!, FrayTestRunner.RecordingFileName)));

            var replay = FrayTestRunner.Run(LostUpdateBody, FrayConfiguration.Replay(exploration.ReportPath!));

            Assert.NotNull(replay.BugFound);
            Assert.Equal(exploration.BugFound!.GetType(), replay.BugFound!.GetType());
            Assert.Equal(exploration.BugFound.Message, replay.BugFound.Message);
            Assert.Equal(1, replay.Iterations);
            Assert.True(replay.ReplayDivergence == null, replay.ReplayDivergence);
        }
        finally
        {
            if (Directory.Exists(reportDirectory))
            {
                Directory.Delete(reportDirectory, recursive: true);
            }
        }
    }

    [Fact]
    public void SeededExplorationIsDeterministic()
    {
        var config1 = new FrayConfiguration { Iterations = 500, Seed = 99 };
        var config2 = new FrayConfiguration { Iterations = 500, Seed = 99 };

        var first = FrayTestRunner.Run(LostUpdateBody, config1);
        var second = FrayTestRunner.Run(LostUpdateBody, config2);

        Assert.NotNull(first.BugFound);
        Assert.NotNull(second.BugFound);
        Assert.Equal(first.Iterations, second.Iterations);
        Assert.NotNull(first.FailingSchedule);
        Assert.NotNull(second.FailingSchedule);
        Assert.Equal(
            first.FailingSchedule!.Select(r => (r.Scheduled, r.Operation)),
            second.FailingSchedule!.Select(r => (r.Scheduled, r.Operation)));
    }

    [Fact]
    public void FifoSchedulerIsReproducibleWithoutSeed()
    {
        // FIFO never interleaves, so the lost update cannot occur.
        var result = FrayTestRunner.Run(LostUpdateBody, new FrayConfiguration
        {
            Scheduler = SchedulerKind.Fifo,
            Iterations = 20,
        });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }
}
