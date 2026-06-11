using System.Text.Json;
using System.Text.Json.Serialization;
using Fray.Core.Observers;
using Fray.Core.Operations;
using Fray.Core.Randomness;

namespace Fray.Core.Scheduling;

/// <summary>
/// Decides which enabled thread executes its pending operation next.
///
/// Mirrors <c>org.pastalab.fray.core.scheduler.Scheduler</c>.
/// </summary>
[JsonPolymorphic(TypeDiscriminatorPropertyName = "type")]
[JsonDerivedType(typeof(RandomScheduler), "random")]
[JsonDerivedType(typeof(FifoScheduler), "fifo")]
[JsonDerivedType(typeof(PctScheduler), "pct")]
[JsonDerivedType(typeof(PosScheduler), "pos")]
public interface IScheduler
{
    /// <param name="threads">Enabled threads, ordered by thread index.</param>
    /// <param name="allThreads">All registered threads of the current execution.</param>
    ThreadContext ScheduleNextOperation(IReadOnlyList<ThreadContext> threads, IReadOnlyCollection<ThreadContext> allThreads);

    /// <summary>Returns the scheduler to use for the next test iteration.</summary>
    IScheduler NextIteration(IRandomness randomness);
}

public static class SchedulerSerialization
{
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    public static string ToJson(IScheduler scheduler) => JsonSerializer.Serialize(scheduler, JsonOptions);

    public static IScheduler FromJson(string json) =>
        JsonSerializer.Deserialize<IScheduler>(json)
        ?? throw new FrayInternalException("Failed to deserialize scheduler.");
}

/// <summary>Picks a uniformly random enabled thread at every step.</summary>
public sealed class RandomScheduler : IScheduler
{
    public IRandomness Rand { get; }

    public RandomScheduler() : this(new ControlledRandom()) { }

    [JsonConstructor]
    public RandomScheduler(IRandomness rand) => Rand = rand;

    public ThreadContext ScheduleNextOperation(IReadOnlyList<ThreadContext> threads, IReadOnlyCollection<ThreadContext> allThreads)
    {
        if (threads.Count == 1)
        {
            return threads[0];
        }
        return threads[Rand.NextInt() % threads.Count];
    }

    public IScheduler NextIteration(IRandomness randomness) => new RandomScheduler(randomness);
}

/// <summary>Always runs the enabled thread with the smallest index (no exploration).</summary>
public sealed class FifoScheduler : IScheduler
{
    public ThreadContext ScheduleNextOperation(IReadOnlyList<ThreadContext> threads, IReadOnlyCollection<ThreadContext> allThreads) =>
        threads[0];

    public IScheduler NextIteration(IRandomness randomness) => this;
}

/// <summary>
/// Probabilistic Concurrency Testing (PCT, Burckhardt et al.): maintains a
/// random thread priority order and lowers the priority of the running thread
/// at a few randomly chosen steps.
///
/// Mirrors <c>org.pastalab.fray.core.scheduler.PCTScheduler</c>.
/// </summary>
public sealed class PctScheduler : IScheduler
{
    public IRandomness Rand { get; }
    public int NumSwitchPoints { get; }
    public int MaxStep { get; private set; }

    [JsonIgnore] private int _currentStep;
    [JsonIgnore] private readonly List<ThreadContext> _threadPriorityQueue = new();
    [JsonIgnore] private readonly HashSet<int> _priorityChangePoints = new();

    public PctScheduler() : this(new ControlledRandom(), 3, 0) { }

    [JsonConstructor]
    public PctScheduler(IRandomness rand, int numSwitchPoints, int maxStep)
    {
        Rand = rand;
        NumSwitchPoints = numSwitchPoints;
        MaxStep = maxStep;
        if (MaxStep != 0)
        {
            PreparePriorityChangePoints();
        }
    }

    public ThreadContext ScheduleNextOperation(IReadOnlyList<ThreadContext> threads, IReadOnlyCollection<ThreadContext> allThreads)
    {
        if (threads.Count == 1)
        {
            return threads[0];
        }
        _currentStep += 1;
        foreach (var thread in threads)
        {
            if (!_threadPriorityQueue.Contains(thread))
            {
                if (_threadPriorityQueue.Count == 0)
                {
                    _threadPriorityQueue.Add(thread);
                    continue;
                }
                var index = Rand.NextInt() % (_threadPriorityQueue.Count + 1);
                _threadPriorityQueue.Insert(index, thread);
            }
        }
        var next = _threadPriorityQueue.First(threads.Contains);
        if (_priorityChangePoints.Contains(_currentStep))
        {
            _threadPriorityQueue.Remove(next);
            _threadPriorityQueue.Add(next);
        }
        return next;
    }

    public IScheduler NextIteration(IRandomness randomness) =>
        new PctScheduler(randomness, NumSwitchPoints, Math.Max(MaxStep, _currentStep));

    private void PreparePriorityChangePoints()
    {
        var steps = Enumerable.Range(1, MaxStep).ToList();
        for (var i = 0; i < NumSwitchPoints; i++)
        {
            var index = Rand.NextInt() % steps.Count;
            _priorityChangePoints.Add(steps[index]);
            steps.RemoveAt(index);
            if (steps.Count == 0)
            {
                break;
            }
        }
    }
}

/// <summary>
/// Partial Order Sampling (POS, Yuan et al.): assigns random priorities and
/// reassigns them for threads whose pending operations race with the chosen
/// operation. Non-racing operations are always scheduled first.
///
/// Mirrors <c>org.pastalab.fray.core.scheduler.POSScheduler</c>.
/// </summary>
public sealed class PosScheduler : IScheduler
{
    public IRandomness Rand { get; }

    [JsonIgnore] private readonly Dictionary<ThreadContext, double> _threadPriority = new();

    public PosScheduler() : this(new ControlledRandom()) { }

    [JsonConstructor]
    public PosScheduler(IRandomness rand) => Rand = rand;

    public ThreadContext ScheduleNextOperation(IReadOnlyList<ThreadContext> threads, IReadOnlyCollection<ThreadContext> allThreads)
    {
        if (threads.Count == 1)
        {
            return threads[0];
        }
        var nonRacing = new List<ThreadContext>();
        foreach (var thread in threads)
        {
            if (!_threadPriority.ContainsKey(thread))
            {
                _threadPriority[thread] = Rand.NextDouble(0.0, 1.0);
            }
            if (thread.PendingOperation is NonRacingOperation)
            {
                nonRacing.Add(thread);
            }
        }
        if (nonRacing.Count > 0)
        {
            return nonRacing.MinBy(t => _threadPriority[t])!;
        }
        var next = threads.MinBy(t => _threadPriority[t])!;
        foreach (var thread in _threadPriority.Keys.ToList())
        {
            var remove = thread == next ||
                         (thread.PendingOperation is RacingOperation racing &&
                          threads.Contains(thread) &&
                          racing.IsRacing(next.PendingOperation));
            if (remove)
            {
                _threadPriority.Remove(thread);
            }
        }
        return next;
    }

    public IScheduler NextIteration(IRandomness randomness) => new PosScheduler(randomness);
}

/// <summary>
/// Replays a previously recorded schedule exactly.
///
/// Mirrors <c>org.pastalab.fray.core.scheduler.ReplayScheduler</c>.
/// </summary>
public sealed class ReplayScheduler : IScheduler
{
    private readonly IReadOnlyList<ScheduleRecording> _recordings;
    private int _index;

    public ReplayScheduler(IReadOnlyList<ScheduleRecording> recordings) => _recordings = recordings;

    public ThreadContext ScheduleNextOperation(IReadOnlyList<ThreadContext> threads, IReadOnlyCollection<ThreadContext> allThreads)
    {
        if (_index >= _recordings.Count)
        {
            return threads[0];
        }
        var recording = _recordings[_index];
        var thread = threads.FirstOrDefault(t => t.Index == recording.Scheduled)
            ?? throw new FrayInternalException(
                $"Replay diverged: scheduled thread {recording.Scheduled} is not enabled at step {_index}.");
        _index++;
        return thread;
    }

    public IScheduler NextIteration(IRandomness randomness) => new ReplayScheduler(_recordings);
}
