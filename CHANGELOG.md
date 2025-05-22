# Changelog

## Unreleased

### Added

- Reenable nix flake build.
- Better stdout and allow customized schedulers.
- Add reactive network controller. The reactive network controller blocks
the thread every time a network operation is performed. Once the operation
is completed, the thread notifies the scheduler to mark the thread as  
runnable. This introduces non-determinism because the schedule depends on 
when the network operation is completed. However this prevents Fray from 
being blocked at network operations.

### Changed

- Make Fray Debugger adaptive to the IDE settings.
- Enable resizable separator to display complete thread names.
- Divide RunContext into controllers.

### Deprecated

### Removed

### Fixed

### Security

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
