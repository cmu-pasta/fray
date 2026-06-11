using System.Diagnostics;
using Fray.Core;
using Fray.Core.Observers;
using Fray.Core.Randomness;
using Fray.Core.Scheduling;

namespace Fray;

/// <summary>Outcome of a Fray test run.</summary>
public sealed class FrayResult
{
    /// <summary>The first bug found, or null when all explored schedules passed.</summary>
    public Exception? BugFound { get; init; }

    /// <summary>Number of executed iterations.</summary>
    public int Iterations { get; init; }

    public long ElapsedMilliseconds { get; init; }

    /// <summary>Where the failing schedule was saved, when a report directory is configured.</summary>
    public string? ReportPath { get; init; }

    /// <summary>Human-readable description of the failure.</summary>
    public string? ErrorReport { get; init; }

    /// <summary>Schedule of the failing iteration (replayable via <see cref="FrayConfiguration.Replay"/>).</summary>
    public IReadOnlyList<ScheduleRecording>? FailingSchedule { get; init; }

    /// <summary>Set when a replay did not follow the recorded schedule.</summary>
    public string? ReplayDivergence { get; init; }

    /// <summary>Throws when a bug was found, with the engine's report attached.</summary>
    public void ThrowIfBugFound()
    {
        if (BugFound != null)
        {
            throw new FrayBugFoundException(this);
        }
    }
}

/// <summary>Wraps the bug found by Fray together with its report.</summary>
public sealed class FrayBugFoundException : Exception
{
    public FrayResult Result { get; }

    public FrayBugFoundException(FrayResult result)
        : base($"Fray found a concurrency bug after {result.Iterations} iteration(s):\n{result.ErrorReport}", result.BugFound)
    {
        Result = result;
    }
}

/// <summary>
/// Runs a test body under controlled concurrency, exploring one schedule per
/// iteration until a bug is found or the iteration budget is exhausted.
///
/// Plays the role of <c>org.pastalab.fray.core.TestRunner</c>.
/// </summary>
public static class FrayTestRunner
{
    public const string RecordingFileName = "recording.json";
    public const string RandomFileName = "random.json";
    public const string ScheduleFileName = "schedule.json";
    public const string ErrorFileName = "error.txt";

    // Fray controls one execution at a time (the runtime is ambient).
    private static readonly object Gate = new();

    public static FrayResult Run(Action body) => Run(body, new FrayConfiguration());

    public static FrayResult Run(Action body, FrayConfiguration config)
    {
        lock (Gate)
        {
            return RunInternal(body, config);
        }
    }

    private static FrayResult RunInternal(Action body, FrayConfiguration config)
    {
        var stopwatch = Stopwatch.StartNew();
        var masterRandom = config.Seed.HasValue ? new Random(config.Seed.Value) : new Random();

        List<ScheduleRecording>? replayRecordings = null;
        ControlledRandom? replayRandom = null;
        if (config.IsReplay)
        {
            replayRecordings = ScheduleRecordings.FromJson(
                File.ReadAllText(Path.Combine(config.ReplayFrom!, RecordingFileName)));
            var randomPath = Path.Combine(config.ReplayFrom!, RandomFileName);
            replayRandom = File.Exists(randomPath)
                ? ControlledRandom.FromJson(File.ReadAllText(randomPath))
                : new ControlledRandom();
        }

        IScheduler? scheduler = null;
        var iterations = config.IsReplay ? 1 : config.Iterations;
        Exception? bugFound = null;
        string? errorReport = null;
        string? reportPath = null;
        string? replayDivergence = null;
        IReadOnlyList<ScheduleRecording>? failingSchedule = null;
        var executedIterations = 0;

        for (var iteration = 0; iteration < iterations; iteration++)
        {
            IRandomness randomness;
            if (config.IsReplay)
            {
                randomness = replayRandom!;
                scheduler = new ReplayScheduler(replayRecordings!);
            }
            else
            {
                randomness = new ControlledRandom(masterRandom.Next());
                scheduler = scheduler == null ? config.CreateScheduler(randomness) : scheduler.NextIteration(randomness);
            }

            var recorder = new ScheduleRecorder();
            var verifier = config.IsReplay ? new ScheduleVerifier(replayRecordings!) : null;
            var observers = new List<IScheduleObserver> { recorder };
            if (verifier != null)
            {
                observers.Add(verifier);
            }

            var context = new RunContext(config, scheduler, randomness, observers);
            Exception? harnessError = null;
            var iterationThread = new Thread(() =>
            {
                FrayRuntime.Install(context);
                try
                {
                    context.Start();
                    try
                    {
                        body();
                    }
                    catch (Exception e)
                    {
                        context.ReportError(e);
                    }
                    context.WaitForAllThreadsToFinish();
                    context.MainExit();
                }
                catch (Exception e)
                {
                    harnessError = e;
                }
                finally
                {
                    FrayRuntime.Uninstall();
                    FrayRuntime.CurrentThreadContext = null;
                }
            })
            {
                IsBackground = true,
                Name = $"fray-iteration-{iteration}",
            };
            iterationThread.Start();
            executedIterations = iteration + 1;

            if (!iterationThread.Join(config.IterationTimeoutMs))
            {
                FrayRuntime.Uninstall();
                var hang = new FrayInternalException(
                    $"Iteration {iteration} did not finish within {config.IterationTimeoutMs}ms. " +
                    "This usually means a controlled thread blocked outside of Fray's primitives.");
                // Prefer a bug the engine already found over the watchdog error.
                bugFound = context.BugFound ?? hang;
                errorReport = (context.ErrorReport ?? "") + hang.Message + "\n" + DumpThreads(context);
                failingSchedule = recorder.RecordingsAtError ?? recorder.Recordings.ToList();
                break;
            }
            if (harnessError != null)
            {
                bugFound = new FrayInternalException($"Fray harness error: {harnessError}");
                errorReport = bugFound.Message;
                break;
            }

            replayDivergence ??= verifier?.Divergence;

            if (context.BugFound != null)
            {
                if (bugFound == null)
                {
                    bugFound = context.BugFound;
                    errorReport = context.ErrorReport;
                    failingSchedule = recorder.RecordingsAtError ?? recorder.Recordings.ToList();
                    if (!config.IsReplay && config.ReportDirectory != null)
                    {
                        reportPath = SaveReport(config, context, scheduler, failingSchedule, errorReport, iteration);
                    }
                }
                if (!config.ExploreMode)
                {
                    break;
                }
            }
        }

        return new FrayResult
        {
            BugFound = bugFound,
            Iterations = executedIterations,
            ElapsedMilliseconds = stopwatch.ElapsedMilliseconds,
            ReportPath = reportPath,
            ErrorReport = errorReport,
            FailingSchedule = failingSchedule,
            ReplayDivergence = replayDivergence,
        };
    }

    private static string DumpThreads(RunContext context)
    {
        var lines = context.RegisteredThreads.Select(t => $"  {t}");
        return "Registered threads:\n" + string.Join("\n", lines);
    }

    private static string SaveReport(FrayConfiguration config, RunContext context, IScheduler scheduler,
        IReadOnlyList<ScheduleRecording> failingSchedule, string? errorReport, int iteration)
    {
        var directory = Path.Combine(config.ReportDirectory!, $"recording_{iteration}");
        Directory.CreateDirectory(directory);
        File.WriteAllText(Path.Combine(directory, RecordingFileName), ScheduleRecordings.ToJson(failingSchedule));
        var randomSnapshot = context.RandomSnapshotAtError;
        if (randomSnapshot != null)
        {
            File.WriteAllText(Path.Combine(directory, RandomFileName), randomSnapshot.ToJson());
        }
        if (scheduler is not ReplayScheduler)
        {
            File.WriteAllText(Path.Combine(directory, ScheduleFileName), SchedulerSerialization.ToJson(scheduler));
        }
        File.WriteAllText(Path.Combine(directory, ErrorFileName),
            $"{errorReport}\nIteration: {iteration}\nStep: {context.Step}\n");
        return directory;
    }
}
