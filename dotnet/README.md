# Fray.NET

A C# port of [Fray](https://github.com/cmu-pasta/fray), the concurrency
testing framework for the JVM. Fray.NET runs multithreaded .NET code under a
deterministic, exploration-based scheduler: one controlled thread executes at
a time, every synchronization operation is a scheduling point, and a pluggable
scheduler decides which thread runs next. Failing schedules are recorded and
can be replayed exactly.

```csharp
var result = FrayTestRunner.Run(() =>
{
    var counter = new FrayShared<int>(0);
    var t1 = FrayThread.StartNew(() => counter.Value = counter.Value + 1);
    var t2 = FrayThread.StartNew(() => counter.Value = counter.Value + 1);
    t1.Join();
    t2.Join();
    if (counter.Value != 2)
    {
        throw new InvalidOperationException($"Lost update: {counter.Value}");
    }
}, new FrayConfiguration { Iterations = 500, Seed = 42 });

result.ThrowIfBugFound(); // fails the test with the schedule that broke it
```

Fray.NET explores a different thread interleaving on every iteration and
stops at the first one that throws, deadlocks, or violates liveness. The
result carries the failing schedule; `FrayConfiguration.Replay(path)` re-runs
it deterministically for debugging.

## Layout

```
dotnet/
  src/Fray/
    Core/                 engine: RunContext, ThreadContext, contexts, operations
    Core/Scheduling/      Random, FIFO, PCT, POS, Replay schedulers
    Core/Observers/       schedule recording, replay verification
    Core/Randomness/      recordable randomness for deterministic replay
    Primitives/           FrayThread, FrayMonitor, FrayLock, FraySemaphore, ...
    Interception/         shims targeted by the IL rewriter (ControlledMonitor, ...)
    FrayTestRunner.cs     iteration loop, report saving, replay
  src/Fray.Rewriter/      Mono.Cecil IL rewriter + `fray-rewrite` CLI
  tests/Fray.TargetCode/  plain multithreaded code (no Fray reference) for rewriter tests
  tests/Fray.Tests/       xUnit suite: known bugs found, correct code passes
```

## Mapping from the JVM implementation

| JVM Fray (`core/`, `runtime/`)                  | Fray.NET                                  |
| ----------------------------------------------- | ----------------------------------------- |
| `RunContext`                                     | `Fray.Core.RunContext`                    |
| `ThreadContext` + `Sync`                         | `Fray.Core.ThreadContext` (semaphore handoff) |
| `concurrency/context/*` (lock/signal/latch/...)  | `Fray.Core.Contexts.*`                    |
| `concurrency/operations/*`                       | `Fray.Core.Operations.*`                  |
| `scheduler/*` (Random, FIFO, PCT, POS, Replay)   | `Fray.Core.Scheduling.*`                  |
| `randomness/ControlledRandom`                    | `Fray.Core.Randomness.ControlledRandom`   |
| `observers/ScheduleRecorder` / `ScheduleVerifier`| `Fray.Core.Observers.*`                   |
| `TestRunner` / `Configuration`                   | `FrayTestRunner` / `FrayConfiguration`    |
| `org.pastalab.fray.runtime.Runtime` delegates    | `FrayRuntime` (ambient context + passthrough) |
| JUnit `@ConcurrencyTest`                         | call `FrayTestRunner.Run` from any test framework |

### Controlled primitives

| Java / .NET primitive                   | Controlled wrapper        |
| ---------------------------------------- | ------------------------- |
| `synchronized` / `Monitor`               | `FrayMonitor`             |
| `ReentrantLock` + `Condition`            | `FrayLock`, `FrayCondition` |
| `Semaphore`                              | `FraySemaphore`           |
| `CountDownLatch` / `CountdownEvent`      | `FrayCountdownEvent`      |
| `ReentrantReadWriteLock` / `ReaderWriterLockSlim` | `FrayReaderWriterLock` |
| `Thread`                                 | `FrayThread`              |
| `AtomicInteger`                          | `FrayAtomicInt32`         |
| volatile / racy shared field             | `FrayShared<T>`           |

Outside of a Fray run every wrapper falls through to the regular .NET
primitive, so the same code runs normally in production-style tests.

## Testing plain code with the IL rewriter

The JVM implementation instruments real concurrency primitives (Java agent +
JDK instrumentation + JVMTI) so arbitrary existing code can be tested.
Fray.NET provides the same capability through static IL rewriting with
Mono.Cecil, the approach proven by Microsoft Coyote:

```bash
dotnet run --project src/Fray.Rewriter -- MyCode.dll -o MyCode.fray.dll
```

The rewriter redirects calls into the `Fray.Interception` shims and inserts
scheduling points around memory accesses:

- `Monitor.Enter/Exit/TryEnter/Wait/Pulse/PulseAll` — including the
  `Monitor.Enter(object, ref bool)` pattern emitted by the C# `lock`
  statement — become `ControlledMonitor` calls
- `new Thread(...)`, `Start`, `Join`, `Interrupt`, `Sleep`, `Yield` become
  `ControlledThread` calls (the real `Thread` object is kept; its body is
  wrapped with the Fray lifecycle protocol)
- the task-parallel subset of `Task` becomes `ControlledTask`:
  `Task.Run(Action)` / `Task.Run<TResult>(Func<TResult>)` bodies execute on
  controlled carrier threads (user code keeps holding a real `Task`), and
  `Wait`, `Result`, `WaitAll`, `IsCompleted` (a yield point, so spin loops
  make progress), and `Task.Delay` become model operations
- `Interlocked.*` becomes `ControlledInterlocked`
- reads and writes of fields *declared in the rewritten assembly* get
  `MemoryHooks` scheduling points (disable with `--no-memory`), so plain
  `counter++` races are explored without any wrappers

The shims check whether the calling thread is controlled, so a rewritten
assembly still runs normally outside of Fray; `Fray.dll` just needs to be
resolvable. `tests/Fray.Tests/RewriterTests.cs` exercises the whole pipeline:
it rewrites `Fray.TargetCode` (plain `Thread`/`lock`/`Monitor`/`Interlocked`
code with no Fray reference), then verifies Fray finds its lost update and
its lock-inversion deadlock — and that its correct code passes and replays
deterministically.

Known rewriter limitations: accesses to fields of value types (structs) and
generic-typed field *stores* are not instrumented; `ldflda`-based access
(e.g. `ref` arguments) is covered only for the intercepted `Interlocked`
methods; constructors are not memory-instrumented. `async`/`await`, task
continuations (`ContinueWith`), and the `Run(Func<Task>)` overloads stay
uncontrolled: waiting on such a task inside a Fray run fails fast with
`NotSupportedException` instead of stalling the exploration.

## Engine vs. instrumentation

The engine never knows which interception mode is in use: wrappers and
rewritten IL both funnel into the same `RunContext` operations. The previous
proof-of-concept in this repository explored runtime interception via the CLR
Profiling API (`ICorProfiler`); that remains an option for attach-time
instrumentation, but static rewriting covers the testing workflow without
native code.

Not yet ported: `StampedLock`, `LockSupport.park/unpark`, NIO/selector
support, the SURW scheduler, timed virtual clock, RMI/MCP/IDE integrations.
Full `async`/`await` support requires rewriting the Task machinery itself
(`AsyncTaskMethodBuilder`, awaiters, continuation scheduling — the deep end
of what Microsoft Coyote does) and is the next candidate milestone.

## Building and testing

```bash
cd dotnet
dotnet test
```

The suite (41 tests, ~2s) checks both directions: seeded explorations *find*
known bugs (lost updates, ABBA deadlocks, lost wakeups, `if`-instead-of-
`while` wait conditions, over-wide semaphores, check-then-act CAS races —
in wrapper-based and in rewritten plain code) and correct implementations
pass hundreds of iterations without false positives. Replay tests verify
that a saved failing schedule reproduces the identical bug and that seeded
runs are fully deterministic.

## Reports and replay

With `ReportDirectory` set, the first failing iteration writes:

- `recording.json` — every scheduling decision up to the bug
- `random.json` — all random values drawn (spurious wakeups, signal choice)
- `schedule.json` — serialized scheduler state
- `error.txt` — the failure and the blocked-thread dump

`FrayConfiguration.Replay(reportPath)` re-runs the recording with a
`ReplayScheduler` and verifies step-by-step that the execution follows it.
