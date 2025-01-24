package org.pastalab.fray.core.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.defaultByName
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.time.TimeSource
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.pastalab.fray.core.logger.FrayLogger
import org.pastalab.fray.core.observers.ScheduleObserver
import org.pastalab.fray.core.observers.ScheduleRecorder
import org.pastalab.fray.core.observers.ScheduleRecording
import org.pastalab.fray.core.observers.ScheduleVerifier
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.scheduler.*

@Serializable
data class ExecutionInfo(
    @Polymorphic val executor: Executor,
    val ignoreUnhandledExceptions: Boolean,
    val interleaveMemoryOps: Boolean,
    val maxScheduledStep: Int,
)

sealed class ExecutionConfig(name: String) : OptionGroup(name) {
  open fun getExecutionInfo(): ExecutionInfo {
    return ExecutionInfo(
        MethodExecutor("", "", emptyList(), emptyList(), emptyMap()), false, false, 10000000)
  }
}

class CliExecutionConfig : ExecutionConfig("cli") {
  val clazz by option().required()
  val method by option().required()
  val targetArgs by
      option("-a", "--args", help = "Arguments passed to target application")
          .split(":")
          .default(emptyList())
  val classpaths by
      option("-cp", "--classpath", help = "Arguments passed to target application")
          .split(":")
          .default(emptyList())
  val ignoreUnhandledExceptions by option("-e", "--ignore-unhandled-exceptions").flag()
  val interleaveMemoryOps by option("-m", "--memory").flag()
  val maxScheduledStep by option("-s", "--max-scheduled-step").int().default(10000)
  val properties by option("-D", help = "System properties").pair().multiple()

  override fun getExecutionInfo(): ExecutionInfo {
    val propertyMap = properties.toMap()
    return ExecutionInfo(
        MethodExecutor(clazz, method, targetArgs, classpaths, propertyMap),
        ignoreUnhandledExceptions,
        interleaveMemoryOps,
        maxScheduledStep)
  }
}

class JsonExecutionConfig : ExecutionConfig("json") {
  val path by option("--config-path").file().required()

  override fun getExecutionInfo(): ExecutionInfo {
    val module = SerializersModule {
      polymorphic(Executor::class) {
        subclass(MethodExecutor::class)
        defaultDeserializer { MethodExecutor.serializer() }
      }
    }
    val json = Json {
      serializersModule = module
      namingStrategy = JsonNamingStrategy.SnakeCase
    }
    return json.decodeFromString<ExecutionInfo>(path.readText())
  }
}

sealed class ScheduleAlgorithm(name: String) : OptionGroup(name) {
  open fun getScheduler(): Triple<Scheduler, ControlledRandom, ScheduleVerifier?> {
    return Triple(FifoScheduler(), ControlledRandom(), null)
  }
}

class Fifo : ScheduleAlgorithm("fifo") {
  override fun getScheduler(): Triple<Scheduler, ControlledRandom, ScheduleVerifier?> {
    return Triple(FifoScheduler(), ControlledRandom(), null)
  }
}

class POS : ScheduleAlgorithm("pos") {
  override fun getScheduler(): Triple<Scheduler, ControlledRandom, ScheduleVerifier?> {
    return Triple(POSScheduler(), ControlledRandom(), null)
  }
}

class Replay : ScheduleAlgorithm("replay") {
  val path by option("--path").file().required()

  override fun getScheduler(): Triple<Scheduler, ControlledRandom, ScheduleVerifier?> {
    val randomPath = "${path.absolutePath}/random.json"
    val schedulerPath = "${path.absolutePath}/schedule.json"
    val randomnessProvider = Json.decodeFromString<ControlledRandom>(File(randomPath).readText())
    val scheduler = Json.decodeFromString<Scheduler>(File(schedulerPath).readText())
    val recordingPath = "${path.absolutePath}/recording.json"
    val scheduleVerifier =
        if (System.getProperty("fray.verifySchedule", "true").toBoolean() &&
            File(recordingPath).exists()) {
          val scheduleRecordings =
              Json.decodeFromString<List<ScheduleRecording>>(File(recordingPath).readText())
          ScheduleVerifier(scheduleRecordings)
        } else {
          null
        }
    return Triple(scheduler, randomnessProvider, scheduleVerifier)
  }
}

class Rand : ScheduleAlgorithm("random") {
  override fun getScheduler(): Triple<Scheduler, ControlledRandom, ScheduleVerifier?> {
    return Triple(RandomScheduler(), ControlledRandom(), null)
  }
}

class PCT : ScheduleAlgorithm("pct") {
  val numSwitchPoints by option().int().default(3)

  override fun getScheduler(): Triple<Scheduler, ControlledRandom, ScheduleVerifier?> {
    return Triple(PCTScheduler(ControlledRandom(), numSwitchPoints, 0), ControlledRandom(), null)
  }
}

class MainCommand : CliktCommand() {
  val report by option("-o", "--output", help = "Report output directory.").default("/tmp/report")
  val timeout by
      option("-t", "--timeout", help = "Testing timeout in seconds.").int().default(60 * 10)
  val iter by option("-i", "--iter", help = "Number of iterations.").int().default(100000)
  val fullSchedule by
      option(
              "-f",
              "--full",
              help =
                  "If the report should save full schedule. Otherwise, Fray only saves schedules points if there are more than one runnable threads.")
          .flag()

  val scheduler by
      option(help = "Scheduling algorithm.")
          .groupChoice(
              "fifo" to Fifo(),
              "pos" to POS(),
              "random" to Rand(),
              "pct" to PCT(),
              "replay" to Replay())
          .defaultByName("random")
  val noFray by option("--no-fray", help = "Runnning in no-Fray mode.").flag()
  val exploreMode by
      option(
              "--explore",
              help = "Running in explore mode and Fray will continue if a failure is found.")
          .flag()
  val noExitWhenBugFound by
      option("--no-exit-on-bug", help = "Fray will not immediately exit when a failure is found.")
          .flag()
  val runConfig by
      option("--run-config", help = "Run configuration for the application.")
          .groupChoice(
              "cli" to CliExecutionConfig(),
              "json" to JsonExecutionConfig(),
          )
          .defaultByName("cli")
  val dummyRun by
      option(
              "--dummy-run",
              help =
                  "Run the target application without dummy run. The dummy run (run target once " +
                      "before launching Fray) helps Fray to prune out non-determinism " +
                      "introduced by the constructors and initializers.")
          .flag()

  override fun run() {}

  fun toConfiguration(): Configuration {
    val executionInfo = runConfig.getExecutionInfo()
    val s = scheduler.getScheduler()
    val configuration =
        Configuration(
            executionInfo,
            report,
            iter,
            timeout,
            s.first,
            s.second,
            fullSchedule,
            exploreMode,
            noExitWhenBugFound,
            scheduler is Replay,
            noFray,
            dummyRun)
    if (s.third != null) {
      configuration.scheduleObservers.add(s.third!!)
    }
    return configuration
  }
}

data class Configuration(
    val executionInfo: ExecutionInfo,
    val report: String,
    val iter: Int,
    val timeout: Int,
    var scheduler: Scheduler,
    var randomnessProvider: ControlledRandom,
    val fullSchedule: Boolean,
    val exploreMode: Boolean,
    var noExitWhenBugFound: Boolean,
    val isReplay: Boolean,
    val noFray: Boolean,
    val dummyRun: Boolean,
) {
  var scheduleObservers = mutableListOf<ScheduleObserver>()
  var nextSavedIndex = 0
  var currentIteration = 0
  val startTime = TimeSource.Monotonic.markNow()

  fun saveToReportFolder(index: Int) {
    Paths.get("$report/recording_$index").createDirectories()
    File("$report/recording_$index/schedule.json").writeText(Json.encodeToString(scheduler))
    File("$report/recording_$index/random.json").writeText(Json.encodeToString(randomnessProvider))
    scheduleObservers.forEach { it.saveToReportFolder("$report/recording_$index") }
  }

  val frayLogger = FrayLogger("$report/fray.log")

  init {
    if (!isReplay || !Paths.get(report).exists()) {
      prepareReportPath(report)
    }
    if (System.getProperty("fray.recordSchedule", "false").toBoolean()) {
      scheduleObservers.add(ScheduleRecorder())
    }

    if (System.getProperty("fray.debugger", "false").toBoolean()) {
      scheduler = FrayIdeaPluginScheduler()
    }
  }

  fun elapsedTime(): Long {
    return (TimeSource.Monotonic.markNow() - startTime).inWholeMilliseconds
  }

  fun shouldRun(): Boolean {
    return elapsedTime() / 1000 <= timeout && currentIteration != iter
  }

  @OptIn(ExperimentalPathApi::class)
  fun prepareReportPath(reportPath: String) {
    val path = Paths.get(reportPath)
    path.deleteRecursively()
    path.createDirectories()
  }
}
