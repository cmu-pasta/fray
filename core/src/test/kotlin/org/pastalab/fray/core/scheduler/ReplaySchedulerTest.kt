package org.pastalab.fray.core.scheduler

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.command.ExecutionInfo
import org.pastalab.fray.core.command.LambdaExecutor
import org.pastalab.fray.core.command.NetworkDelegateType
import org.pastalab.fray.core.command.SystemTimeDelegateType
import org.pastalab.fray.core.observers.ScheduleRecording
import org.pastalab.fray.core.observers.TimelineCoverageType
import org.pastalab.fray.core.randomness.ControlledRandomProvider

class ReplaySchedulerTest {
  @Test
  fun reportsDivergenceWhenRecordedThreadIsNotRunnable() {
    val scheduler =
        ReplayScheduler(
            listOf(ScheduleRecording(1, listOf(1), ""), ScheduleRecording(2, listOf(2), ""))
        )
    val context = context(scheduler)
    try {
      val thread = ThreadContext(Thread(), 1, context, 0)
      assertSame(thread, scheduler.scheduleNextOperation(listOf(thread), listOf(thread)))
      val error =
          assertFailsWith<IllegalStateException> {
            scheduler.scheduleNextOperation(listOf(thread), listOf(thread))
          }
      assertEquals(
          "Replay diverged at recording index 1: scheduled thread 2 is not runnable; runnable threads: [1]",
          error.message,
      )
      assertEquals(1, scheduler.index)
    } finally {
      context.shutDown()
    }
  }

  @Test
  fun usesFirstRunnableThreadAfterRecordingEnds() {
    val scheduler = ReplayScheduler(emptyList())
    val context = context(scheduler)
    try {
      val thread = ThreadContext(Thread(), 1, context, 0)
      assertSame(thread, scheduler.scheduleNextOperation(listOf(thread), listOf(thread)))
    } finally {
      context.shutDown()
    }
  }

  private fun context(scheduler: Scheduler): RunContext =
      RunContext(
          Configuration(
              ExecutionInfo(LambdaExecutor({}), false, false, -1),
              Files.createTempDirectory("fray-replay-test"),
              1,
              1000,
              scheduler,
              ControlledRandomProvider(),
              fullSchedule = false,
              exploreMode = false,
              noExitWhenBugFound = true,
              isReplay = true,
              noFray = false,
              dummyRun = false,
              networkDelegateType = NetworkDelegateType.NONE,
              systemTimeDelegateType = SystemTimeDelegateType.NONE,
              virtualTimeDelta = 100_000L,
              ignoreTimedBlock = true,
              sleepAsYield = false,
              resetClassLoader = true,
              redirectStdout = false,
              abortThreadExecutionAfterMainExit = false,
              resolveRacingOperationStackTraceHash = false,
              timelineCoverageType = TimelineCoverageType.NONE,
          )
      )
}
