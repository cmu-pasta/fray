using System.Text.Json;

namespace Fray.Core.Observers;

/// <summary>Observes scheduling decisions during an execution.</summary>
public interface IScheduleObserver
{
    void OnExecutionStart();

    void OnNewSchedule(IReadOnlyList<ThreadContext> allThreads, ThreadContext scheduled);

    void OnContextSwitch(ThreadContext current, ThreadContext next);

    void OnReportError(Exception exception);

    void OnExecutionDone(Exception? bugFound);
}

/// <summary>One recorded scheduling decision.</summary>
public sealed record ScheduleRecording(int Scheduled, List<int> Enabled, string Operation);

public static class ScheduleRecordings
{
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    public static string ToJson(IReadOnlyList<ScheduleRecording> recordings) =>
        JsonSerializer.Serialize(recordings, JsonOptions);

    public static List<ScheduleRecording> FromJson(string json) =>
        JsonSerializer.Deserialize<List<ScheduleRecording>>(json)
        ?? throw new FrayInternalException("Failed to deserialize schedule recordings.");
}

/// <summary>
/// Records every scheduling decision so a failing execution can be replayed
/// with <see cref="Scheduling.ReplayScheduler"/>.
///
/// Mirrors <c>org.pastalab.fray.core.observers.ScheduleRecorder</c>.
/// </summary>
public sealed class ScheduleRecorder : IScheduleObserver
{
    private readonly List<ScheduleRecording> _recordings = new();

    /// <summary>Recordings at the moment the bug was reported (excludes shutdown noise).</summary>
    public List<ScheduleRecording>? RecordingsAtError { get; private set; }

    public IReadOnlyList<ScheduleRecording> Recordings => _recordings;

    public void OnExecutionStart()
    {
        _recordings.Clear();
        RecordingsAtError = null;
    }

    public void OnNewSchedule(IReadOnlyList<ThreadContext> allThreads, ThreadContext scheduled)
    {
        var enabled = allThreads.Select(t => t.Index).ToList();
        _recordings.Add(new ScheduleRecording(scheduled.Index, enabled, scheduled.PendingOperation.GetType().Name));
    }

    public void OnContextSwitch(ThreadContext current, ThreadContext next) { }

    public void OnReportError(Exception exception) => RecordingsAtError ??= new List<ScheduleRecording>(_recordings);

    public void OnExecutionDone(Exception? bugFound) { }
}

/// <summary>
/// Verifies during replay that scheduling decisions match a recording;
/// reports divergence as an internal error.
///
/// Mirrors <c>org.pastalab.fray.core.observers.ScheduleVerifier</c>.
/// </summary>
public sealed class ScheduleVerifier : IScheduleObserver
{
    private readonly IReadOnlyList<ScheduleRecording> _schedules;
    private int _index;

    /// <summary>First divergence found, if any.</summary>
    public string? Divergence { get; private set; }

    public ScheduleVerifier(IReadOnlyList<ScheduleRecording> schedules) => _schedules = schedules;

    public void OnExecutionStart() => _index = 0;

    public void OnNewSchedule(IReadOnlyList<ThreadContext> allThreads, ThreadContext scheduled)
    {
        if (_index >= _schedules.Count || Divergence != null)
        {
            return;
        }
        var recording = _schedules[_index];
        var enabled = allThreads.Select(t => t.Index).ToList();
        var operation = scheduled.PendingOperation.GetType().Name;
        if (recording.Scheduled != scheduled.Index)
        {
            Divergence = $"Step {_index}: scheduled thread mismatch, expected {recording.Scheduled}, got {scheduled.Index}.";
        }
        else if (!recording.Enabled.SequenceEqual(enabled))
        {
            Divergence = $"Step {_index}: registered thread set mismatch, expected [{string.Join(",", recording.Enabled)}], got [{string.Join(",", enabled)}].";
        }
        else if (recording.Operation != operation)
        {
            Divergence = $"Step {_index}: operation mismatch, expected {recording.Operation}, got {operation}.";
        }
        _index++;
    }

    public void OnContextSwitch(ThreadContext current, ThreadContext next) { }

    public void OnReportError(Exception exception) { }

    public void OnExecutionDone(Exception? bugFound) { }
}
