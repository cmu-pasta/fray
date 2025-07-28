package org.pastalab.fray.core.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.defaultByName
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
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
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.debugger.DebuggerRegistry
import org.pastalab.fray.core.logger.FrayLogger
import org.pastalab.fray.core.observers.AntithesisErrorReporter
import org.pastalab.fray.core.observers.ScheduleRecorder
import org.pastalab.fray.core.observers.ScheduleRecording
import org.pastalab.fray.core.observers.ScheduleVerifier
import org.pastalab.fray.core.observers.ThreadPausingTimeLogger
import org.pastalab.fray.core.randomness.AntithesisSdkRandomProvider
import org.pastalab.fray.core.randomness.ControlledRandomProvider
import org.pastalab.fray.core.randomness.Randomness
import org.pastalab.fray.core.randomness.RandomnessProvider
import org.pastalab.fray.core.randomness.RecordedRandomProvider
import org.pastalab.fray.core.scheduler.*
import org.pastalab.fray.rmi.Constant.FRAY_DEBUGGER_DEBUG
import org.pastalab.fray.rmi.Constant.FRAY_DEBUGGER_DISABLED
import org.pastalab.fray.rmi.Constant.FRAY_DEBUGGER_PROPERTY_KEY
import org.pastalab.fray.rmi.Constant.FRAY_DEBUGGER_REPLAY
import org.pastalab.fray.rmi.ScheduleObserver
import org.pastalab.fray.rmi.TestStatusObserver

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
        MethodExecutor("", "", emptyList(), emptyList(), emptyMap()),
        false,
        false,
        10000000,
    )
  }
}

class EmptyExecutionConfig : ExecutionConfig("empty") {
  override fun getExecutionInfo(): ExecutionInfo {
    return ExecutionInfo(
        LambdaExecutor {},
        false,
        false,
        -1,
    )
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
        maxScheduledStep,
    )
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
      @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
      namingStrategy = JsonNamingStrategy.SnakeCase
    }
    return json.decodeFromString<ExecutionInfo>(path.readText())
  }
}

sealed class RandomnessProviderOption(name: String) : OptionGroup(name) {
  open fun getRandomnessProvider(): RandomnessProvider {
    return ControlledRandomProvider()
  }
}

class ControlledRandomOption : RandomnessProviderOption("controlled-random") {
  override fun getRandomnessProvider(): RandomnessProvider {
    return ControlledRandomProvider()
  }
}

class RecordedRandomOption : RandomnessProviderOption("recorded-random") {
  val path by option("--path-to-random").file().required()

  override fun getRandomnessProvider(): RandomnessProvider {
    return RecordedRandomProvider(path.absolutePath)
  }
}

class AntithesisSdkRandomOption : RandomnessProviderOption("antithesis-sdk-random") {
  override fun getRandomnessProvider(): RandomnessProvider {
    return AntithesisSdkRandomProvider()
  }
}

sealed class ScheduleAlgorithm(name: String, val isReplay: Boolean) : OptionGroup(name) {
  open fun getScheduler(randomness: Randomness): Scheduler {
    return FifoScheduler()
  }
}

class Fifo : ScheduleAlgorithm("fifo", false) {
  override fun getScheduler(randomness: Randomness): Scheduler {
    return FifoScheduler()
  }
}

class POS : ScheduleAlgorithm("pos", false) {
  override fun getScheduler(randomness: Randomness): Scheduler {
    return POSScheduler(randomness)
  }
}

class ReplayFromRecordings : ScheduleAlgorithm("replay-from-recordings", true) {
  val path by option("--path-to-recordings").file().required()

  override fun getScheduler(randomness: Randomness): Scheduler {
    val recordingPath = "${path.absolutePath}/recording.json"
    val scheduleRecordings =
        Json.decodeFromString<List<ScheduleRecording>>(File(recordingPath).readText())
    return ReplayScheduler(scheduleRecordings)
  }
}

class Replay : ScheduleAlgorithm("replay-from-scheduler", true) {
  val path by option("--path-to-scheduler").file().required()

  override fun getScheduler(randomness: Randomness): Scheduler {
    val schedulerPath = "${path.absolutePath}/schedule.json"
    return Json.decodeFromString<Scheduler>(File(schedulerPath).readText())
  }
}

class Rand : ScheduleAlgorithm("random", false) {
  override fun getScheduler(randomness: Randomness): Scheduler {
    return RandomScheduler(randomness)
  }
}

class PCT : ScheduleAlgorithm("pct", false) {
  val numSwitchPoints by option().int().default(3)
  val maxStep by option().int().default(0)

  override fun getScheduler(randomness: Randomness): Scheduler {
    return PCTScheduler(randomness, numSwitchPoints, maxStep)
  }
}

class SURW : ScheduleAlgorithm("surw", false) {
  override fun getScheduler(randomness: Randomness): Scheduler {
    System.setProperty("fray.resolveRacingOperationStackTraceHash", "true")
    return SURWScheduler(randomness, mutableMapOf(), mutableSetOf())
  }
}

class Dynamic : ScheduleAlgorithm("dynamic", false) {
  val schedulerName by option("--scheduler-name")

  override fun getScheduler(randomness: Randomness): Scheduler {
    val clazz = this::class.java.classLoader.loadClass(schedulerName)
    val scheduler =
        clazz.getConstructor(Randomness::class.java).newInstance(randomness) as Scheduler
    return scheduler
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
                  "If the report should save full schedule. Otherwise, Fray only saves schedules points if there are more than one runnable threads.",
          )
          .flag()

  val scheduler by
      option(help = "Scheduling algorithm.")
          .groupChoice(
              "fifo" to Fifo(),
              "pos" to POS(),
              "random" to Rand(),
              "pct" to PCT(),
              "surw" to SURW(),
              "dynamic" to Dynamic(),
              "replay-from-recordings" to ReplayFromRecordings(),
              "replay" to Replay(),
          )
          .defaultByName("random")
  val randomnessProvider by
      option(help = "Randomness provider.")
          .groupChoice(
              "controlled-random" to ControlledRandomOption(),
              "recorded-random" to RecordedRandomOption(),
              "antithesis-sdk-random" to AntithesisSdkRandomOption(),
          )
          .defaultByName("controlled-random")
  val noFray by option("--no-fray", help = "Runnning in no-Fray mode.").flag()
  val exploreMode by
      option(
              "--explore",
              help = "Running in explore mode and Fray will continue if a failure is found.",
          )
          .flag()
  val noExitWhenBugFound by
      option("--no-exit-on-bug", help = "Fray will not immediately exit when a failure is found.")
          .flag()
  val runConfig by
      option("--run-config", help = "Run configuration for the application.")
          .groupChoice(
              "cli" to CliExecutionConfig(),
              "json" to JsonExecutionConfig(),
              "empty" to EmptyExecutionConfig(),
          )
          .defaultByName("cli")
  val dummyRun by
      option(
              "--dummy-run",
              help =
                  "Run the target application without dummy run. The dummy run (run target once " +
                      "before launching Fray) helps Fray to prune out non-determinism " +
                      "introduced by the constructors and initializers.",
          )
          .flag()
  val networkDelegateType by
      option(
              "--network-delegate-type",
              help =
                  "Network delegate type. Possible values: PROACTIVE, REACTIVE, NONE. " +
                      "Default is PROACTIVE.",
          )
          .choice(
              "proactive" to NetworkDelegateType.PROACTIVE,
              "reactive" to NetworkDelegateType.REACTIVE,
              "none" to NetworkDelegateType.NONE,
          )
          .default(NetworkDelegateType.REACTIVE)
  val systemTimeDelegateType by
      option(
              "--system-time-delegate-type",
              help = "Time delegate type. Possible values: MOCK, NONE. " + "Default is MOCK.",
          )
          .choice(
              "mock" to SystemTimeDelegateType.MOCK,
              "none" to SystemTimeDelegateType.NONE,
          )
          .default(SystemTimeDelegateType.NONE)
  val ignoreTimedBlock by
      option(
              "--ignore-timed-block",
              help =
                  "Ignore timed block in the target application (e.g., Thread.sleep) will be " +
                      "unblocked immediately.",
          )
          .flag("--no-ignore-timed-block", default = true)
  val sleepAsYield by
      option("--sleep-as-yield", help = "Treat Thread.sleep as Thread.yield.")
          .flag("--no-sleep-as-yield", default = false)

  override fun run() {}

  fun toConfiguration(): Configuration {
    val executionInfo = runConfig.getExecutionInfo()
    val randomnessProvider = randomnessProvider.getRandomnessProvider()
    val configuration =
        Configuration(
            executionInfo,
            report,
            iter,
            timeout,
            scheduler.getScheduler(randomnessProvider.getRandomness()),
            randomnessProvider,
            fullSchedule,
            exploreMode,
            noExitWhenBugFound,
            scheduler.isReplay,
            noFray,
            dummyRun,
            networkDelegateType,
            systemTimeDelegateType,
            ignoreTimedBlock,
            sleepAsYield)
    val recordingPath = System.getProperty("fray.verifySchedule")
    if (recordingPath != null && scheduler.isReplay) {
      val recordings =
          Json.decodeFromString<List<ScheduleRecording>>(File(recordingPath).readText())
      val verifier = ScheduleVerifier(recordings)
      configuration.scheduleObservers.add(verifier)
      configuration.testStatusObservers.add(verifier)
    }
    if (System.getProperty("fray.antithesisSdk", "false").toBoolean()) {
      configuration.testStatusObservers.add(AntithesisErrorReporter())
    }
    return configuration
  }
}

data class Configuration(
    val executionInfo: ExecutionInfo,
    val report: String,
    var iter: Int,
    val timeout: Int,
    var scheduler: Scheduler,
    val randomnessProvider: RandomnessProvider,
    val fullSchedule: Boolean,
    val exploreMode: Boolean,
    var noExitWhenBugFound: Boolean,
    val isReplay: Boolean,
    val noFray: Boolean,
    val dummyRun: Boolean,
    val networkDelegateType: NetworkDelegateType,
    val systemTimeDelegateType: SystemTimeDelegateType,
    val ignoreTimedBlock: Boolean,
    val sleepAsYield: Boolean,
) {
  val scheduleObservers = mutableListOf<ScheduleObserver<ThreadContext>>()
  val testStatusObservers = mutableListOf<TestStatusObserver>()
  var nextSavedIndex = 0
  var currentIteration = 0
  val startTime = TimeSource.Monotonic.markNow()
  var randomness = randomnessProvider.getRandomness()

  fun saveToReportFolder(index: Int): String {
    val path =
        if (exploreMode) {
          "$report/recording_$index"
        } else {
          "$report/recording"
        }
    Paths.get(path).createDirectories()
    File("$path/schedule.json").writeText(Json.encodeToString(scheduler))
    File("$path/random.json").writeText(Json.encodeToString(randomness))
    testStatusObservers.forEach { it.saveToReportFolder(path) }
    return path
  }

  val frayLogger = FrayLogger("$report/fray.log")

  init {
    if (!isReplay || !Paths.get(report).exists()) {
      prepareReportPath(report)
    }
    if (System.getProperty("fray.recordSchedule", "false").toBoolean()) {
      val scheduleRecorder = ScheduleRecorder()
      testStatusObservers.add(scheduleRecorder)
      scheduleObservers.add(scheduleRecorder)
    }
    if (System.getProperty("fray.logPausingTime", "false").toBoolean()) {
      scheduleObservers.add(ThreadPausingTimeLogger(frayLogger))
    }

    val debuggerProperty = System.getProperty(FRAY_DEBUGGER_PROPERTY_KEY, FRAY_DEBUGGER_DISABLED)
    if (debuggerProperty != FRAY_DEBUGGER_DISABLED) {
      if (debuggerProperty == FRAY_DEBUGGER_DEBUG) {
        scheduler = FrayIdeaPluginScheduler(null)
      } else {
        assert(debuggerProperty == FRAY_DEBUGGER_REPLAY) {
          "Invalid value for $FRAY_DEBUGGER_PROPERTY_KEY: $debuggerProperty"
        }
        scheduler = FrayIdeaPluginScheduler(scheduler)
      }
      testStatusObservers.add(DebuggerRegistry.getRemoteScheduleObserver())
      iter = 1
    }
  }

  fun elapsedTime(): Long {
    return (TimeSource.Monotonic.markNow() - startTime).inWholeMilliseconds
  }

  fun shouldRun(): Boolean {
    return elapsedTime() / 1000 <= timeout && currentIteration != iter
  }

  fun nextIteration() {
    currentIteration++
    scheduler = scheduler.nextIteration(randomnessProvider.getRandomness())
    randomness = randomnessProvider.getRandomness()
  }

  @OptIn(ExperimentalPathApi::class)
  fun prepareReportPath(reportPath: String) {
    val path = Paths.get(reportPath)
    path.deleteRecursively()
    path.createDirectories()
  }
}
