using Fray.Core.Randomness;
using Fray.Core.Scheduling;

namespace Fray;

public enum SchedulerKind
{
    /// <summary>Uniformly random choice at every scheduling point.</summary>
    Random,

    /// <summary>Deterministic first-come-first-served execution (no exploration).</summary>
    Fifo,

    /// <summary>Probabilistic Concurrency Testing (random priorities + switch points).</summary>
    Pct,

    /// <summary>Partial Order Sampling (prioritizes racing operations).</summary>
    Pos,
}

/// <summary>
/// Configuration of a Fray test run.
///
/// Plays the role of <c>org.pastalab.fray.core.command.Configuration</c>.
/// </summary>
public sealed class FrayConfiguration
{
    /// <summary>Maximum number of schedules to explore.</summary>
    public int Iterations { get; set; } = 100;

    public SchedulerKind Scheduler { get; set; } = SchedulerKind.Random;

    /// <summary>Overrides <see cref="Scheduler"/> with a custom scheduler.</summary>
    public Func<IRandomness, IScheduler>? SchedulerFactory { get; set; }

    /// <summary>Seed for reproducible exploration; random when null.</summary>
    public int? Seed { get; set; }

    /// <summary>Aborts an iteration that exceeds this many scheduling steps (liveness guard). 0 disables.</summary>
    public int MaxScheduledSteps { get; set; } = 100_000;

    /// <summary>Do not treat unhandled exceptions in controlled threads as bugs.</summary>
    public bool IgnoreUnhandledExceptions { get; set; }

    /// <summary>Model <see cref="FrayThread.Sleep"/> as a yield instead of a timed block.</summary>
    public bool SleepAsYield { get; set; }

    /// <summary>
    /// Treat timeouts of timed operations as schedulable only when no other
    /// thread can run (keeps executions independent of wall-clock time).
    /// </summary>
    public bool IgnoreTimedBlock { get; set; } = true;

    /// <summary>Explore spurious wakeups of waits, as permitted by monitor semantics.</summary>
    public bool AllowSpuriousWakeups { get; set; } = true;

    /// <summary>Abort remaining controlled threads once the test body returned.</summary>
    public bool AbortThreadsAfterMainExit { get; set; } = true;

    /// <summary>Keep exploring after the first bug instead of stopping.</summary>
    public bool ExploreMode { get; set; }

    /// <summary>Directory where failing schedules are saved; null disables reports.</summary>
    public string? ReportDirectory { get; set; }

    /// <summary>Watchdog for a single iteration; guards against engine hangs.</summary>
    public int IterationTimeoutMs { get; set; } = 60_000;

    /// <summary>Path of a saved report to replay instead of exploring.</summary>
    public string? ReplayFrom { get; set; }

    public bool IsReplay => ReplayFrom != null;

    /// <summary>Configuration that replays the schedule saved at <paramref name="reportPath"/>.</summary>
    public static FrayConfiguration Replay(string reportPath) => new() { ReplayFrom = reportPath };

    internal IScheduler CreateScheduler(IRandomness randomness)
    {
        if (SchedulerFactory != null)
        {
            return SchedulerFactory(randomness);
        }
        return Scheduler switch
        {
            SchedulerKind.Random => new RandomScheduler(randomness),
            SchedulerKind.Fifo => new FifoScheduler(),
            SchedulerKind.Pct => new PctScheduler(randomness, numSwitchPoints: 3, maxStep: 0),
            SchedulerKind.Pos => new PosScheduler(randomness),
            _ => throw new ArgumentOutOfRangeException(nameof(Scheduler)),
        };
    }
}
