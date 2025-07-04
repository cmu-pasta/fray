# Changelog

## Unreleased

### Added

- Allow Fray to use real-world time to decide if an operation should be unblocked.
- Skip class constructors in `sun/security/ssl/SSLContextImpl`
- Add instrumentation points at `NioSocket::accept`
- Add thread pausing time logger.
- Add java agent launcher to use fray through java agent (can only run 1 iteration). 

### Changed

- Implement more efficient schedule recorder/verifier.
- Instrument sleep statements in java.util.concurrent
- Introduce `onSkipPrimitive` and `onSkipScheduling` to better support
internal primitives and class constructors.
- Only compute the stack trace hash when using SURW algorithm

### Deprecated

### Removed

### Fixed

- Fix incorrect blocking time for `Unsafe.park(false, 0)`.
- Fix thread created in the `onSkipMethod` state is not tracked.
- Fix concurrent modification exception in read write lock.
- Fix `/tmp` does not exist error on macOS.

### Security

## 0.5.1 - 2025-06-02

### Changed

- Better skip logic in JUnit tests and Intellij debugger.

### Fixed

- Fix Fray crash due to unimplemented methods in `ProactiveNetworkController`.

## 0.5.0 - 2025-06-02

### Added

- Reenable nix flake build.
- Better stdout and allow customized schedulers.
- Add reactive network controller. The reactive network controller blocks
the thread every time a network operation is performed. Once the operation
is completed, the thread notifies the scheduler to mark the thread as  
runnable. This introduces non-determinism because the schedule depends on 
when the network operation is completed. However this prevents Fray from 
being blocked at network operations.
- Add `verifyNoThrow` and `mustBeCaught` methods to verify that no exceptions 
are thrown from Fray.
- Add `runInFray` and `runInFrayDone` methods to run code in Fray and 
avoid recursive calls.
- Fix Fray hang if a thread is interrupted in ConditionWakeBlocked state.
- Track new condition creation even if in the skip method state.
- Add `-verify` while running integration tests.

### Changed

- Make Fray Debugger adaptive to the IDE settings.
- Enable resizable separator to display complete thread names.
- Divide RunContext into controllers.
- Add ClassConstructorInstrumenter to the JDK instrumenter.
- Use `WeakIdentityHashMap` in `ReferenceContextManager` for more robust
reference management.
- Use a `enabledOperationBuffer` in `RunContext` to avoid creating new 
list for every scheduling point.
- Update `SkipMethodInstrumenter` and `ClassConstructorInstrumenter` to 
avoid Fray hanging.
- Update `MethodExitInstrumenter` to provide better exception handling.
- Update `MonitorInstrumenter` to support `HttpClient`.
- Fix `VerifyError` caused by instrumenters.

## 0.4.4 - 2025-05-12

### Added

- Implement support for non-blocking and blocking I/O operations using ServerSocketChannel, SocketChannel, 
and Selector classes, enabling connect and accept operations.
- Support NIO write and read operations.

### Changed

- Remove assertions in `NioContextManager` as channels can be safely closed multiple times.

### Fixed

- Fix Stamped Lock missing `onSkipMethod` instrumentation.

## 0.4.3 - 2025-05-01

### Added

- Skip interleavings inside MethodHandle\[s\].

### Changed

- Rename syncurity to ranger.
- Support Intellij 252.

### Fixed

- Fix fray command line duplicate `--path` exception.
- Delete fray log file when fray command line is run, fixing stale logs.
- Use gcc to build jvmti.so on Darwin as well.
- Fix Fray hang while using condition in static field.
- Fix Fray hang when two threads deadlock each other through condition wait.

## 0.4.2 - 2025-04-16

### Changed

- Link libc++ and libgcc statically.

## 0.4.1 - 2025-04-13

### Fixed

- Fix JDK folder detection on Linux and improve JDK version handling.
- Fix build on NixOS.
- Use system JDK when running on NixOS.

## 0.4.0 - 2025-04-09

### Added

- Implement [SURW](https://dl.acm.org/doi/10.1145/3669940.3707214) algorithm
- Add `stackTraceHash` to `RacingOperations` to track the creation location of racing operations.
- Implement the MCP server for the Fray Debugger plugin.
- Introduce replay mode for the Fray debugger plugin.
- Introduce schedule replayer.
- Support Java 23

### Changed

- Simplify the timeline visualization in replay mode.
- Disable the `onNewSchedule` for the Fray debugger plugin to improve the performance.

### Removed

- Move deadlock empire to a separate repository.

### Fixed

- Fix Fray hang when security manager is enabled.
- Fix wrong hover position calculation in the Fray Debugger plugin.
- Fix concurrent modification exception in the `ThreadTimelinePanel`.
- Fix replay when Intellij debugger is attached.
- Fix `ObjectInstrumenter` visitor visits `methodExitLabel` twice exception.

## 0.3.1 - 2025-03-11

### Added

- Allow the Fray debugger to highlight all lines of a thread that is being executed.
- Enable click action in the thread stack trace to navigate to the source code.
- Add translated Deadlock Empire games!
- Change the focus of the editor when a new thread is selected in the Fray debugger.

### Changed

- Simplify the `mainExit` logic and introduce `MainExiting` state.
- Change the z order of highlighting to be on top of the editor.

### Fixed

- Fix Fray hang when a thread exits but the monitor lock is not released.

## 0.3.0 - 2025-03-10

### Added

- Pass the `ResourceInfo` to the operations that may block a
  thread's execution.
- Add `ThreadResourcePanel` to the Fray Debugger plugin. The panel visualizes the concurrency resources of each thread.

### Changed

- Refine the timeline construction logic. Pass timeline information through
  schedule observers.
- Improve the condition/object wait blocking by using the original lock instead
  of the while loop.

### Fixed

- Disable Fray instrumentation when `System.exit` is called. This avoids
  deadlock when a new thread is created during the shutdown process.
- Fixed Github release action including the wrong changelog.
- Prevent Fray hangs by properly removing threads from read/write lock waiters
- Fix Fray hangs if wait/await are used in syncurity conditions.

## 0.2.8 - 2025-02-26

### Added

- Add a new timeline view in the Intellij Plugin.

### Changed

- Assign different colors to different threads in the Intellij Plugin.
- Rename the thread state to `Runnabled` and `Blocked`.
- Release the Fray Debugger plugin through github actions.
- Append changelog to the release notes in the github action.

### Fixed

- Fix deadlock while evaluating syncurity conditions. This happens if the condition
  contains synchronization primitives. The solution is to switch Runtime delegate to
  a special syncurity delegate that does not call RunContext directly.

## 0.2.7 - 2025-02-24

### Added

- Initial release of Fray Debugger plugin.

### Changed

- Update `docs/IDE.md` to include Fray Debugger plugin.
- Fix Fray Debugger crashes when the line number is negative.
