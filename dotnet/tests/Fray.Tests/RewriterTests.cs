using System.Reflection;
using Fray.Core;
using Fray.Rewriter;
using Xunit;

namespace Fray.Tests;

/// <summary>
/// Rewrites the plain (Fray-free) <c>Fray.TargetCode</c> assembly once and
/// loads it for all rewriter tests.
/// </summary>
public sealed class RewrittenAssemblyFixture
{
    public Assembly Assembly { get; }
    public RewriteResult Result { get; }

    public RewrittenAssemblyFixture()
    {
        var input = Path.Combine(AppContext.BaseDirectory, "Fray.TargetCode.dll");
        var directory = Path.Combine(Path.GetTempPath(), $"fray-rewrite-{Guid.NewGuid():N}");
        Directory.CreateDirectory(directory);
        var output = Path.Combine(directory, "Fray.TargetCode.Rewritten.dll");
        Result = AssemblyRewriter.Rewrite(new RewriterOptions
        {
            InputPath = input,
            OutputPath = output,
            NewAssemblyName = "Fray.TargetCode.Rewritten",
        });
        Assembly = Assembly.LoadFrom(output);
    }
}

public class RewriterTests : IClassFixture<RewrittenAssemblyFixture>
{
    private readonly RewrittenAssemblyFixture _fixture;

    public RewriterTests(RewrittenAssemblyFixture fixture) => _fixture = fixture;

    private Action Target(string methodName)
    {
        var type = _fixture.Assembly.GetType("Fray.TargetCode.PlainTargets")!;
        return (Action)type.GetMethod(methodName)!.CreateDelegate(typeof(Action));
    }

    [Fact]
    public void RewritesCallsAndFields()
    {
        Assert.True(_fixture.Result.RedirectedCalls > 0, "no calls were redirected");
        Assert.True(_fixture.Result.InstrumentedFieldAccesses > 0, "no field accesses were instrumented");
    }

    [Fact]
    public void FindsLostUpdateInPlainThreadCode()
    {
        var result = FrayTestRunner.Run(Target("LostUpdate"), new FrayConfiguration
        {
            Iterations = 500,
            Seed = 42,
        });

        Assert.NotNull(result.BugFound);
        Assert.IsType<InvalidOperationException>(result.BugFound);
        Assert.Contains("Lost update", result.BugFound!.Message);
    }

    [Fact]
    public void PlainLockStatementPreventsTheRace()
    {
        var result = FrayTestRunner.Run(Target("LockedUpdate"), new FrayConfiguration
        {
            Iterations = 100,
            Seed = 7,
        });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }

    [Fact]
    public void FindsDeadlockInPlainNestedLocks()
    {
        var result = FrayTestRunner.Run(Target("DeadlockProne"), new FrayConfiguration
        {
            Iterations = 1000,
            Seed = 3,
        });

        Assert.NotNull(result.BugFound);
        Assert.IsType<DeadlockException>(result.BugFound);
    }

    [Fact]
    public void PlainMonitorWaitPulseWorks()
    {
        var result = FrayTestRunner.Run(Target("WaitAndPulse"), new FrayConfiguration
        {
            Iterations = 150,
            Seed = 17,
        });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }

    [Fact]
    public void PlainInterlockedHasNoRace()
    {
        var result = FrayTestRunner.Run(Target("InterlockedUpdate"), new FrayConfiguration
        {
            Iterations = 100,
            Seed = 7,
        });

        Assert.True(result.BugFound == null, result.ErrorReport);
    }

    [Fact]
    public void RewrittenAssemblyStillRunsUncontrolled()
    {
        // Outside a Fray run all shims fall through to the real primitives.
        Target("LockedUpdate")();
        Target("InterlockedUpdate")();
    }

    [Fact]
    public void RewrittenLostUpdateReplaysToTheSameBug()
    {
        var reportDirectory = Path.Combine(Path.GetTempPath(), $"fray-report-{Guid.NewGuid():N}");
        try
        {
            var exploration = FrayTestRunner.Run(Target("LostUpdate"), new FrayConfiguration
            {
                Iterations = 500,
                Seed = 1234,
                ReportDirectory = reportDirectory,
            });
            Assert.NotNull(exploration.BugFound);
            Assert.NotNull(exploration.ReportPath);

            var replay = FrayTestRunner.Run(Target("LostUpdate"), FrayConfiguration.Replay(exploration.ReportPath!));

            Assert.NotNull(replay.BugFound);
            Assert.Equal(exploration.BugFound!.Message, replay.BugFound!.Message);
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
}
