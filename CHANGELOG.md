# Changelog

## Unreleased

### Added

- Pass the `ResourceInfo` to the operations that may block a
thread's execution.
- Add `ThreadResourcePanel` to the Fray Debugger plugin. The panel visualizes the concurrency resources of each thread.

### Changed

- Refine the timeline construction logic. Pass timeline information through
schedule observers.

### Deprecated

### Removed

### Fixed

- Disable Fray instrumentation when `System.exit` is called. This avoids 
  deadlock when a new thread is created during the shutdown process.
- Fixed Github release action including the wrong changelog.
- Prevent Fray hangs by properly removing threads from read/write lock waiters
- Fix Fray hangs if wait/await are used in syncurity conditions.

### Security

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
