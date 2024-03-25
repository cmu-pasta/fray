There are three different ways to instrument a concurrency primitive.

## Reactive Thread Blocking

For primitives that only block the thread when it is acquired by another thread.
SFuzz uses a shadow lock to mimic the behaviour of those primitives and only 
proceed when the thread is given that lock.

- E.g. `ReentrantLock.lock`
- Before `ReentrantLock.lock` is called, SFuzz will first block if the lock is acquired by 
  others. It will only unblock when `ReentrantLock.lock` succeeds. This makes `ReentrantLock.lock`
  itself non-blocking. We do not want to remove the call to `ReentrantLock.lock` because it
  also maintains the metadata of the `ReentrantLock`.
- The instrumented method becomes a noop for non-controlled threads (e.g. GC threads), this 
  instrumentation will not break any non-controlled threads.
- Targets: locks, memory operations


## Proactive Locking

For primitives that block the thread when it is called, we use the original method call to block the thread.

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
    // Calling `o.wait()` will release the monitor lock
    // and we should also unlock the monitor lock here
    // so that other threads can acquire the lock.
    lockManager.unlockMonitor(o);

    objectWaiters[o].add(Thread.currentThread());
    do {
        // We want to signal that this thread goes
        // back to the while loop again.
        s?.signal();
        o.wait();
    } while (isRunning(Thread.currentThread()));

    // If the thread is unblocked, it means that
    // the thread receives the notify signal and
    // successfully acquire the monitor lock.
    // Thus, we should remove the thread from
    // monitorWaiter and also lock the monitor lock.
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
    // If this thread is blocked by `o.wait()`
    // we can only unblock it through `o.notifyAll()`
    // Note that this will wake all threads that are
    // blocked by `o.wait()`. This is fine because
    // Those threads will be blocked again through while
    // loop.
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

Lock manager mimic the behaviour of Reentrant lock. It should also update the threads who are
blocked by `o.wait()`.

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
