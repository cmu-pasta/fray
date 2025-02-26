# Changelog

## Unreleased

### Added

- Add a new timeline view in the Intellij Plugin.

### Changed

- Assign different colors to different threads in the Intellij Plugin.
- Rename the thread state to `Runnabled` and `Blocked`.

### Deprecated

### Removed

### Fixed

- Fix deadlock while evaluating syncurity conditions. This happens if the condition
contains synchronization primitives. The solution is to switch Runtime delegate to 
a special syncurity delegate that does not call RunContext directly. 

### Security

## 0.2.7 - 2025-02-24

### Added

- Initial release of Fray Debugger plugin.

### Changed

- Update `docs/IDE.md` to include Fray Debugger plugin.
- Fix Fray Debugger crashes when the line number is negative.
