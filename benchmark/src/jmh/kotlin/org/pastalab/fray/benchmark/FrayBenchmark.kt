package org.pastalab.fray.benchmark

import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import org.pastalab.fray.core.TestRunner
import org.pastalab.fray.core.command.Configuration
import org.pastalab.fray.core.command.ExecutionInfo
import org.pastalab.fray.core.command.MethodExecutor
import org.pastalab.fray.core.command.NetworkDelegateType
import org.pastalab.fray.core.command.SystemTimeDelegateType
import org.pastalab.fray.core.randomness.ControlledRandomProvider
import org.pastalab.fray.core.scheduler.FifoScheduler
import org.pastalab.fray.core.scheduler.PCTScheduler
import org.pastalab.fray.core.scheduler.POSScheduler
import org.pastalab.fray.core.scheduler.RandomScheduler
import org.pastalab.fray.core.scheduler.Scheduler

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
open class FrayBenchmark {

  @Param("8") var threadCount: Int = 8

  @Param("pos") var schedulerName: String = "pos"

  @Param(
      "LockContention",
      "ProducerConsumer",
      "MonitorContention",
      "CountDownLatchBarrier",
      "ThreadCreation",
      "VolatileContention",
  )
  var workload: String = "LockContention"

  @Benchmark
  fun bench() {
    val classpath = System.getProperty("java.class.path").split(File.pathSeparator)
    val scheduler = createScheduler(schedulerName)
    val config =
        Configuration(
            ExecutionInfo(
                MethodExecutor(
                    "org.pastalab.fray.benchmark.workload.$workload",
                    "run",
                    listOf(threadCount.toString()),
                    classpath,
                    mapOf(),
                ),
                false,
                false,
                -1,
            ),
            Path.of(System.getProperty("java.io.tmpdir"), "fray-bench"),
            1000,
            60,
            scheduler,
            ControlledRandomProvider(),
            true,
            false,
            true,
            false,
            false,
            false,
            NetworkDelegateType.REACTIVE,
            SystemTimeDelegateType.MOCK,
            100_000L,
            true,
            false,
            false,
            false,
            false,
        )
    TestRunner(config).run()
  }

  companion object {
    fun createScheduler(name: String): Scheduler =
        when (name) {
          "fifo" -> FifoScheduler()
          "random" -> RandomScheduler()
          "pct" -> PCTScheduler()
          "pos" -> POSScheduler()
          else -> throw IllegalArgumentException("Unknown scheduler: $name")
        }
  }
}
