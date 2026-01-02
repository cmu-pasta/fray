# Fray Agent Notes

This project is Fray, a Java concurrency testing platform for the JVM. It instruments the JVM and application,
controls thread scheduling (PCT, POS, random), and supports deterministic replay for debugging.

## Repo Map

- `core/`: scheduling logic, runtime delegates, concurrency contexts, and schedulers.
- `runtime/`: JVM/application instrumentation points and delegate interfaces.
- `instrumentation/`: Java agent + JDK instrumentation (jlink plugin).
- `junit/`: JUnit 5 integration and annotations like `@ConcurrencyTest`.
- `mcp/`: MCP server that connects to the Fray runtime over RMI.
- `plugins/idea/`: IntelliJ debugger plugin.
- `bin/`: launchers (e.g., `bin/fray`) for running tests with Fray.

## Key Concepts

- Fray intercepts concurrency primitives and routes them through runtime delegates, which decide whether
  Fray should control the operation (skipping JVM/system threads and modeled primitives).
- `RunContext` in `core/` is the central execution state: thread context, concurrency context, and scheduler state.
- Schedulers implement `Scheduler.scheduleNextOperation` and may be stateless or stateful (`Random`, `PCT`, `POS`).

## Running Tests

- JUnit 5: add `@ExtendWith(FrayTestExtension.class)` and annotate test methods with `@ConcurrencyTest`.
- Gradle: run `./gradlew frayTest` to execute Fray-annotated tests.
- Maven: use the Fray Maven plugin and add `fray-junit` as a test dependency (see `README.md`).

## Debugging and Repro

- Fray reports failing schedules in a report directory; you can replay by passing `replay = "PATH"` to
  `@ConcurrencyTest`. For exact interleavings, record schedules with `-Dfray.recordSchedule=true` and use
  `ReplayScheduler`.
- When Fray hangs, inspect `RunContext.currentThreadId` and use `jstack -l <pid>`. Deadlocks can occur when
  JVM-internal locks are used by both the JVM and the app. Fixes often involve adding classes to
  `SkipPrimitiveInstrumenter` or `SkipScheduleInstrumenter`.

## Platform Notes

- Fray downloads Corretto JDK 25 by default. On NixOS, set `JDK25_HOME` to an OpenJDK 25 install.

## Documentation Pointers

- Usage guide: `docs/usage.md`
- Architecture overview: `docs/architecture.md`
- IDE + debugger plugin: `docs/IDE.md`
- MCP workflow: `docs/mcp.md`
