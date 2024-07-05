package cmu.pasta.fray.core.command

import cmu.pasta.fray.core.logger.CsvLogger
import cmu.pasta.fray.core.logger.JsonLogger
import cmu.pasta.fray.core.logger.LoggerBase
import cmu.pasta.fray.core.scheduler.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.defaultByName
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import java.util.*
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Serializable
data class ExecutionInfo(
    @Polymorphic val executor: Executor,
    val ignoreUnhandledExceptions: Boolean,
    val timedOpAsYield: Boolean,
    val interleaveMemoryOps: Boolean,
    val maxScheduledStep: Int,
)

sealed class ExecutionConfig(name: String) : OptionGroup(name) {
  open fun getExecutionInfo(): ExecutionInfo {
    return ExecutionInfo(
        MethodExecutor("", "", emptyList(), emptyList(), emptyMap()), false, false, false, 10000000)
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
  val timedOpAsYield by option("-t", "--timed-op-as-yield").flag()
  val ignoreUnhandledExceptions by option("-e", "--ignore-unhandled-exceptions").flag()
  val interleaveMemoryOps by option("-m", "--memory").flag()
  val maxScheduledStep by option("-s", "--max-scheduled-step").int().default(10000)
  val properties by option("-D", help = "System properties").pair().multiple()

  override fun getExecutionInfo(): ExecutionInfo {
    val propertyMap = properties.toMap()
    return ExecutionInfo(
        MethodExecutor(clazz, method, targetArgs, classpaths, propertyMap),
        ignoreUnhandledExceptions,
        timedOpAsYield,
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

sealed class Logger(name: String) : OptionGroup(name) {
  open fun getLogger(baseFolder: String, fullSchedule: Boolean): LoggerBase {
    return JsonLogger(baseFolder, fullSchedule)
  }
}

class JsonLoggerOption : Logger("json") {
  override fun getLogger(baseFolder: String, fullSchedule: Boolean): LoggerBase {
    return JsonLogger(baseFolder, fullSchedule)
  }
}

class CsvLoggerOption : Logger("csv") {
  override fun getLogger(baseFolder: String, fullSchedule: Boolean): LoggerBase {
    return CsvLogger(baseFolder, fullSchedule)
  }
}

sealed class ScheduleAlgorithm(name: String) : OptionGroup(name) {
  open fun getScheduler(): Scheduler {
    return FifoScheduler()
  }
}

class Replay : ScheduleAlgorithm("replay") {
  val path by option().file().required()

  override fun getScheduler(): Scheduler {
    return ReplayScheduler(Schedule.fromString(path.readText(), path.extension == "json", false))
  }
}

class Fifo : ScheduleAlgorithm("fifo") {
  override fun getScheduler(): Scheduler {
    return FifoScheduler()
  }
}

class POS : ScheduleAlgorithm("pos") {
  override fun getScheduler(): Scheduler {
    return POSScheduler(Random())
  }
}

class Rand : ScheduleAlgorithm("random") {
  override fun getScheduler(): Scheduler {
    return RandomScheduler()
  }
}

class PCT : ScheduleAlgorithm("pct") {
  val numSwitchPoints by option().int().default(3)

  override fun getScheduler(): Scheduler {
    return PCTScheduler(ControlledRandom(), numSwitchPoints)
  }
}

class MainCommand : CliktCommand() {
  val report by option("-o").default("/tmp/report")
  val iter by option("-i", "--iter", help = "Number of iterations").int().default(1)
  val fullSchedule by option("-f", "--full").flag()
  val logger by
      option("-l", "--logger").groupChoice("json" to JsonLoggerOption(), "csv" to CsvLoggerOption())
  val scheduler by
      option()
          .groupChoice(
              "replay" to Replay(),
              "fifo" to Fifo(),
              "pos" to POS(),
              "random" to Rand(),
              "pct" to PCT())
  val noFray by option("--no-fray").flag()
  val runConfig by
      option()
          .groupChoice(
              "cli" to CliExecutionConfig(),
              "json" to JsonExecutionConfig(),
          )
          .defaultByName("cli")

  override fun run() {}

  fun toConfiguration(): Configuration {
    val executionInfo = runConfig.getExecutionInfo()
    return Configuration(
        executionInfo,
        report,
        iter,
        scheduler!!.getScheduler(),
        fullSchedule,
        logger!!.getLogger(report, fullSchedule),
        noFray)
  }
}

data class Configuration(
    val executionInfo: ExecutionInfo,
    val report: String,
    val iter: Int,
    val scheduler: Scheduler,
    val fullSchedule: Boolean,
    val logger: LoggerBase,
    val noFray: Boolean = false
) {}
