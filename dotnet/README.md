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
    FrayTestRunner.cs     iteration loop, report saving, replay
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

## The one architectural difference

The JVM implementation instruments real concurrency primitives (Java agent +
JDK instrumentation + JVMTI), so arbitrary existing code can be tested, and
the engine has to coordinate with the real primitives (helper thread,
synchronization points, `sendSignalToObject`). Fray.NET v1 instead *models*
primitives entirely inside the engine: a controlled thread only ever parks on
its own `ThreadContext` signal, which removes that coordination machinery but
requires code under test to use the `Fray*` wrappers.

The previous proof-of-concept in this repository explored the
instrumentation route via the CLR Profiling API (`ICorProfiler`). That—or
IL rewriting with Mono.Cecil, as done by Microsoft Coyote—remains the natural
next step to lift the wrapper requirement; the engine underneath would stay
exactly as it is, since `RunContext` only sees model operations either way.

Not yet ported: `StampedLock`, `LockSupport.park/unpark`, NIO/selector
support, the SURW scheduler, timed virtual clock, RMI/MCP/IDE integrations.
`Task`/`async` support requires IL rewriting (uncontrolled blocking inside the
thread pool cannot be intercepted by wrappers) and is out of scope for v1.

## Building and testing

```bash
cd dotnet
dotnet test
```

The suite (27 tests, ~1s) checks both directions: seeded explorations *find*
known bugs (lost updates, ABBA deadlocks, lost wakeups, `if`-instead-of-
`while` wait conditions, over-wide semaphores, check-then-act CAS races) and
correct implementations pass hundreds of iterations without false positives.
Replay tests verify that a saved failing schedule reproduces the identical
bug and that seeded runs are fully deterministic.

## Reports and replay

With `ReportDirectory` set, the first failing iteration writes:

- `recording.json` — every scheduling decision up to the bug
- `random.json` — all random values drawn (spurious wakeups, signal choice)
- `schedule.json` — serialized scheduler state
- `error.txt` — the failure and the blocked-thread dump

`FrayConfiguration.Replay(reportPath)` re-runs the recording with a
`ReplayScheduler` and verifies step-by-step that the execution follows it.
