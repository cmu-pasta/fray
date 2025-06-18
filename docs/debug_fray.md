# Debugging Fray 


One major issue Fray faces is deadlocks. This is because Fray does not instrument concurrency primitives used by the [JVM](https://github.com/search?q=repo%3Aopenjdk%2Fjdk+ObjectSynchronizer++OR+ObjectLocker+language%3AC%2B%2B+&type=code). So if a concurrency primitive is used by both the application and the JVM, it leads to deadlocks. For example, if Fray yields inside a class constructor, and another threads tries to construct the same class, it will deadlock. 

Fray's current solution is when Fray tries to acquire a lock that Fray knows the JVM may also acquire, it will skip scheduling for the current thread until the current thread is blocked or the lock is released. This is done by introducing the `onSkipScheduling` method in the runtime delegates. When Fray calls the `onSkipScheduling` method, it will run the current thread as long as possible, until the current thread is blocked or calls `onSkipSchedulingDone`. However, this is not a perfect solution, as it may still lead to deadlocks in some cases.


So when Fray hangs, you can try the following steps to debug. First you may attach a debugger to the JVM and try to find the state of Fray by inspecting the `RunContext` object and view the `currentThreadId`. Next you may navigate to the current scheduled thread to understand why this thread is blocked. `jstack` is a useful tool to get the stack trace of all threads in the JVM. You can use `jstack -l <pid>` to get the stack trace of all threads in the JVM, where `<pid>` is the process ID of the JVM. 

If you find that the scheduled thread is in `RUNNABLE` state and the stack trace shows that it is going to execute a normal instruction but the JVM is not making any progress. It is likely that JVM is waiting for a lock that is held by another thread (e.g., a [class loader](https://github.com/openjdk/jdk/blob/c4fb00a7be51c7a05a29d3d57d787feb5c698ddf/src/hotspot/share/classfile/systemDictionary.cpp#L604)).

Most issues can be resolved by adding the corresponding classes to the [SkipPrimitiveInstrumenter](https://github.com/cmu-pasta/fray/blob/main/instrumentation/base/src/main/kotlin/org/pastalab/fray/instrumentation/base/visitors/SkipPrimitiveInstrumenter.kt) and [SkipScheduleInstrumenter](https://github.com/cmu-pasta/fray/blob/main/instrumentation/base/src/main/kotlin/org/pastalab/fray/instrumentation/base/visitors/SkipScheduleInstrumenter.kt). Please also submit your changes as a pull request to the Fray repository so that we can improve Fray for everyone.
