# Thread Wait/Notify

For thread wait and notify, we rely on the original `wait` to block a thread
this requires us to maintain the `wait` and `notify` information manually.
Here is an example:

Thread t1:

```java
synchronized (o) {
    // T1 part 1
    o.notify();
    // T1 part 2
}
```

Thread t2:

```java
synchronized (o) {
    // T2 part 1
    o.wait();
    // T2 part 2
}
```


Instrumented t1:
```java
synchronized (o) {
    // T1 part 1
    // We want to pick one thread that
    // receives the notify signal and move
    // it to the `monitorWaiters[o]`
    Thread t = objectWaiters[o].pick();
    monitorWaiters[o].add(t);
    o.notify();
    // T1 part 2

    // Before MONITOREXIT(o) is executed


    // We first need to compute how
    // many threads are blocked by
    // `o.wait()`
    s = barrier(objectWaiters[o].size + monitorWatiers[o].size);
    // Next we call `o.notifyAll`, which makes previous
    // `o.notify()` and `o.notifyAll()` a no-op.
    o.notifyAll()
}
// After MONITOREXIT(o) is executed.
s.wait()
s = null;
lockManager.unlockMonitor(o)
```

Instrumented t2:

```java
lockManager.lockMonitor(o)
synchronized (o) {
    // T2 part 1
    lockManager.unlockMonitor(o);
    objectWaiters[o].add(Thread.currentThread());
    do {
        // We want to signal that this thread goes
        // back to the while loop again.
        s?.signal();
        o.wait();
    } while (isRunning(Thread.currentThread()));
    monitorWaiters[o].remove(Thread.currentThread());
    lockManager.lockMonitor(o);
    // T2 part 2
}
```

Schedule Algorithm:
```java
Thread t = pickRunnableThread();
t.setRunning();
if (t.isBlockedByObjectWait()) {
    Object o = t.getObject();
    o.notifyAll();
} else {
    ...
}
if (!Thread.currentThread().isRunning()) {
    blockCurrentThread();
}
```

The key idea is to use `do { o.wait() } while (...)` to block a thread until it is scheduled to run.
This allows JVM to release the monitor lock acquired by the current thread and avoiding deadlocks.
When a `o.notify` is called, we mimic the behaviour of notify and pick one thread from `objectWaiters`
and put it to `monitorWaiters`. We can't enable the notified thread immediately because it still waits
for the monitor lock.

Finally, when the running thread releases the monitor lock, it will set all threads in `monitorWaiters` to
`Runnable` and allow it to reschedule again.



Lock Manager Detail:

Lock manager mimic the behaviour of Reentrant lock.

`monitorWaiters` tracks all threads that has received `o.notify()` but waiting for monitor lock.
`objectWaiters` trackes all threads that waits for `o.notify()`.


Note that the `unlockMonitor` should unlock completely if it is called from
`object.wait`. (i.e., if
`MONITORENTER (o)` is called multiple times, it should unlock all of them).

```java
void lockMonitor(o) {
    ...
    monitorWaiters[o].forEach(t -> t.setBlocked());
}
void unlockMonitor(o) {
    ...
    monitorWaiters[o].forEach(t -> t.setRunnable());
}
```




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