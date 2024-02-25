# Runtime

Runtime should be **super** light weight. It should
not include any dependencies. It should not call any JDK methods.

# Threads

Each thread should only try to block itself.

# Locks

Thread blocking/resuming should be managed at Thread level instead of sync information level.

We should use `wati` and `notify` to block/unblock a thread because we will instrument semaphores as well.

```java
synchronized (monitor) {
    monitor.wait(); 
}
synchronized (monitor) {
        monitor.notify(); 
}
```

`Thread.join` should be handled the same way as `Object.wait`

# Instrumentation

Instrumentation should be as simple as possible. Ideally only static method calls.

We should have different classes to instrument different types of code so that they can be disabled easily.

# Synchronization

We should **strictly** enforce sequential execution. If a new thread is created/resumed, we
should wait for them to finish bootstrap and fully stopped before proceeding.

Thread rescheduling can only be called by the current scheduled thread.
