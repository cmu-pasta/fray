# Changelog

## Unreleased

### Added

### Changed

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
