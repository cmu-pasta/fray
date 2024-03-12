# Thread Park/Unpark

## Instrumentation

- `onThreadPark` at the beginning of each `park*` method in `LockSupport`
- `onThreadParkDone` at the end of each `park*` method in `LockSupport`
- `onUnpark` at the beginning of `LockSupport.unpark`
- `onUnpark` at the end of `LockSupport.unpark`

## Logic

For thread park, SFuzz will first check if unpark is signaled for the given thread.
If true, SFuzz continue executing the current thread. Otherwise, SFuzz set the status 
of the current thread to `Paused` and call `scheduleNextOperation(false)`. Note that SFuzz
will not block current thread through `ThreadContext.block` because the `park` method 
will block the thread itself.


When `unpark` is called from another thread, 