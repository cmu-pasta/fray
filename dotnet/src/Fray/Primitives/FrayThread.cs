using Fray.Core;
using Fray.Core.Operations;

namespace Fray;

/// <summary>
/// A thread whose scheduling is controlled by Fray during a test run. Outside
/// of a Fray run it behaves like a plain <see cref="Thread"/>, so the same
/// code can execute normally in production-style tests.
/// </summary>
public sealed class FrayThread
{
    private static int _nextId;

    private readonly Action _body;
    private readonly Thread _thread;
    private readonly ManualResetEventSlim _started = new(false);
    private RunContext? _runContext;
    private ThreadContext? _context;
    private volatile bool _completed;

    public string Name { get; }

    public FrayThread(Action body, string? name = null)
    {
        _body = body;
        Name = name ?? $"fray-thread-{Interlocked.Increment(ref _nextId)}";
        _thread = new Thread(RunBody) { IsBackground = true, Name = Name };
    }

    public static FrayThread StartNew(Action body, string? name = null)
    {
        var thread = new FrayThread(body, name);
        thread.Start();
        return thread;
    }

    public void Start()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            _thread.Start();
            return;
        }
        _runContext = runContext;
        _context = runContext.ThreadCreate(_thread, Name);
        _thread.Start();
        // Rendezvous: the child registered itself and parked; mirrors the
        // threadStart/threadStartDone synchronization of the JVM runtime.
        _started.Wait();
    }

    private void RunBody()
    {
        var runContext = _runContext;
        if (runContext == null)
        {
            _body();
            return;
        }
        var context = _context!;
        runContext.ThreadRun(context, _started);
        try
        {
            _body();
        }
        catch (TargetTerminateException)
        {
            // The execution is winding down (bug found or main exited).
        }
        catch (Exception e)
        {
            if (!runContext.Config.IgnoreUnhandledExceptions)
            {
                runContext.ReportError(e);
            }
        }
        finally
        {
            _completed = true;
            runContext.ThreadCompleted(this, context);
        }
    }

    public void Join()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null || !ReferenceEquals(runContext, _runContext))
        {
            _thread.Join();
            return;
        }
        // Java-style join: wait on the thread handle's monitor until completed.
        runContext.MonitorEnter(this);
        try
        {
            while (!_completed)
            {
                runContext.ObjectWait(this, BlockedOperation.NotTimed, canInterrupt: true);
            }
        }
        finally
        {
            runContext.MonitorExit(this);
        }
    }

    public void Interrupt()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null || !ReferenceEquals(runContext, _runContext))
        {
            _thread.Interrupt();
            return;
        }
        runContext.ThreadInterrupt(_context!);
    }

    /// <summary>Controlled <see cref="Thread.Sleep(int)"/>: a timed block under Fray.</summary>
    public static void Sleep(int milliseconds)
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Thread.Sleep(milliseconds);
            return;
        }
        runContext.ThreadSleep(milliseconds);
    }

    /// <summary>Controlled <see cref="Thread.Yield"/>: a schedule point under Fray.</summary>
    public static void Yield()
    {
        var runContext = FrayRuntime.ControlledContext();
        if (runContext == null)
        {
            Thread.Yield();
            return;
        }
        runContext.Yield();
    }
}
