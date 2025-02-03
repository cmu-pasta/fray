import org.jetbrains.kotlinx.lincheck.ExperimentalModelCheckingAPI
import org.jetbrains.kotlinx.lincheck.runConcurrentTest
import org.junit.jupiter.api.Test
import org.pastalab.fray.test.fail.cdl.CountDownLatchDeadlockUnblockMultiThread
import org.pastalab.fray.test.fail.cdl.CountDownLatchDeadlockUnblockSameThread
import org.pastalab.fray.test.fail.intstream.IntStream
import org.pastalab.fray.test.fail.monitor.MonitorDeadlock
import org.pastalab.fray.test.fail.monitor.SynchronizedMethodDeadlock
import org.pastalab.fray.test.fail.park.ParkDeadlock
import org.pastalab.fray.test.fail.park.ParkSpuriousWakeup
import org.pastalab.fray.test.fail.rwlock.ReentrantReadWriteLockDeadlock
import org.pastalab.fray.test.fail.stampedlock.StampedLockConversionDeadlock
import org.pastalab.fray.test.fail.stampedlock.StampedLockDeadlock
import org.pastalab.fray.test.fail.wait.NotifyOrder
import org.pastalab.fray.test.fail.wait.RescheduleBeforeWaitReacquireMonitorLock
import org.pastalab.fray.test.fail.wait.WaitSpuriousWakeup
import org.pastalab.fray.test.fail.wait.WaitWithoutMonitorLock
import org.pastalab.fray.test.fail.wait.WaitWithoutNotifyDeadlock
import org.pastalab.fray.test.success.cdl.CountDownLatchAwaitTimeoutNoDeadlock
import org.pastalab.fray.test.success.cdl.CountDownLatchCountDownBeforeAwait
import org.pastalab.fray.test.success.cdl.CountDownLatchNormalNotify
import org.pastalab.fray.test.success.rwlock.ReentrantReadWriteLockNoDeadlock
import org.pastalab.fray.test.success.thread.ThreadInterruptionWithoutStart

class LincheckTestKt {
  val invocations = 10000;
  @OptIn(ExperimentalModelCheckingAPI::class)
  @Test
  fun test() = runConcurrentTest(invocations, this::mytest)

  fun mytest() {
//    .main(null);
  }
}

