# Sync Points in System.out/err

While using SFuzz with JUnit tests. JUnit is launched through 
gradle wrappers which will initialize System.out/err before 
SFuzz starts. This will cause SFuzz to miss Condition object 
creation and will not be able to sync on the condition object.

```
java.lang.NullPointerException
	at cmu.pasta.sfuzz.core.concurrency.ReentrantLockMonitor.lockFromCondition(ReentrantLockMonitor.kt:82)
	at cmu.pasta.sfuzz.core.GlobalContext.conditionSignalAll(GlobalContext.kt:306)
	at cmu.pasta.sfuzz.core.RuntimeDelegate.onConditionSignalAll(RuntimeDelegate.kt:186)
	at java.base/cmu.pasta.sfuzz.runtime.Runtime.onConditionSignalAll(Runtime.java:81)
	at java.base/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.signalAll(AbstractQueuedSynchronizer.java)
	at org.gradle.internal.remote.internal.hub.queue.EndPointQueue.dispatch(EndPointQueue.java:41)
	at org.gradle.internal.remote.internal.hub.queue.MultiEndPointQueue.flush(MultiEndPointQueue.java:94)
	at org.gradle.internal.remote.internal.hub.queue.MultiEndPointQueue.dispatch(MultiEndPointQueue.java:48)
	at org.gradle.internal.remote.internal.hub.MessageHub$ChannelDispatch.dispatch(MessageHub.java:373)
	at org.gradle.internal.dispatch.ProxyDispatchAdapter$DispatchingInvocationHandler.invoke(ProxyDispatchAdapter.java:94)
	at jdk.proxy3/jdk.proxy3.$Proxy6.output(Unknown Source)
	at org.gradle.api.internal.tasks.testing.processors.TestOutputRedirector$Forwarder.onOutput(TestOutputRedirector.java:75)
	at org.gradle.api.internal.tasks.testing.processors.DefaultStandardOutputRedirector$WriteAction.text(DefaultStandardOutputRedirector.java:89)
	at org.gradle.internal.io.LineBufferingOutputStream.flush(LineBufferingOutputStream.java:96)
	at org.gradle.internal.io.LineBufferingOutputStream.write(LineBufferingOutputStream.java:80)
	at java.base/java.io.OutputStream.write(OutputStream.java:167)
	at java.base/java.io.PrintStream.implWrite(PrintStream.java:643)
	at java.base/java.io.PrintStream.write(PrintStream.java:623)
	at java.base/sun.nio.cs.StreamEncoder.writeBytes(StreamEncoder.java:309)
	at java.base/sun.nio.cs.StreamEncoder.implFlushBuffer(StreamEncoder.java:405)
	at java.base/sun.nio.cs.StreamEncoder.lockedFlushBuffer(StreamEncoder.java:123)
	at java.base/sun.nio.cs.StreamEncoder.flushBuffer(StreamEncoder.java:110)
	at java.base/java.io.OutputStreamWriter.flushBuffer(OutputStreamWriter.java:192)
	at java.base/java.io.PrintStream.implWrite(PrintStream.java:812)
	at java.base/java.io.PrintStream.write(PrintStream.java:790)
	at java.base/java.io.PrintStream.print(PrintStream.java:1002)
	at org.gradle.internal.io.LinePerThreadBufferingOutputStream.println(LinePerThreadBufferingOutputStream.java:240)
	at example.ThreadInterruptTest$1.run(ThreadInterruptTest.java:20)
```

We should avoid using printf statements in the unit tests. 
Try to use `Logger` instead.