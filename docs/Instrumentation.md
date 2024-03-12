There are three different ways to instrument a concurrency primitive.

## Before Method Call

- E.g. `ReentrantLock.lock`
- Before `ReentrantLock.lock` is called, SFuzz will first block if the lock is acquired by 
  others. It will only unblock when `ReentrantLock.lock` succeeds. This makes `ReentrantLock.lock`
  itself non-blocking. We do not want to remove the call to `ReentrantLock.lock` because it
  also maintains the metadata of the `ReentrantLock`.
- The instrumented method becomes a noop for non-controlled threads (e.g. GC threads), this 
  instrumentation will not break any non-controlled threads.
- Targets: locks, memory operations

> [!WARNING]
> If you decided to not removing the original method call, you need to make sure 
  the call will not block the thread. Otherwise, it will deadlock.


## Replacing Method Call

- E.g. `Object.wait`
- SFuzz will replace `Object.wait` with `onObjectWait(o)` in both application and JDK. 
  If `onObjectWait(o)` is called from a SFuzz-controlled thread, it will block the thread 
  and perform a reschedule. Otherwise, it calls `o.wait()`.
- Why replacing the original `o.wait()` statement in SFuzz-controlled threads?
  - Consider the scenario when multiple threads call `o.wait` and one thread calls `o.notify`. 
  JVM will wake one thread randomly. To control this, SFuzz removes `o.wait`.
- Is this replacing safe?
  - Yes. 
  - `o.wait` does not modify the internal state of an `Object` and removing it will not affect 
  memory.
  - If a thread is not controlled by SFuzz, `o.wait` will be called again. 
  
> [!WARNING]
> Removing a method call will cause side effect. For example `LockSupport.park(o)` will first 
> set the blocker of the parked thread to `o` and then park the thread. If you want to replace 
> this method, you need to make sure that SFuzz maintains this information faithfully.


## Let the Primitive Block!

- E.g. `Condition.await`
- Before `Condition.await` is called, SFuzz will set the state of current thread to `Paused` and
  run reschedule algorithm, however, it will not block the thread through `ThreadContext.block` because
  `Condition.await` will block the current thread.
- We can't remove the `Condition.await` because it changes the state of the `Condition` object. Removing
  the method call will change the state of the object.
- We can't block before `Condition.await` because it is a proactive lock. It blocks until another threads
  calls `Condition.signal`.
- How to unblock?
  - When another threads calls `Condition.singal`, the blocked thread will resume executing automatically.
  To prevent this we also insert a method call `onConditionAwaitDone` at the end of `Condition.await`.
  It will block the thread using `ThreadContext.block` and set the state of the thread to `Enabled`.
- Enforcing sequential execution:
  -  To enforce sequential execution, the thread calls `Condition.signal` should be blocked until 
  `onConditionAwaitDone` finishes.
- Multiple waits:
  - If there are multiple threads calls `Condition.await`, SFuzz needs pick which thread to enable. To achieve
  this, after `Condition.signal` is called, SFuzz calls `Condition.signalAll` again to wake up all threads and 
  decides which thread should be unblocked.
  - In `onConditionAwaitDone`, if a thread is enabled, it sets its state to `Enabled` and calls 
  `ThreadContext.block` to block itself. Otherwise, it calls `Condition.await` to block itself.


> [!WARNING]
> This is the most confusion way to control a thread. Please consider the first two strategies before 
> using this.
