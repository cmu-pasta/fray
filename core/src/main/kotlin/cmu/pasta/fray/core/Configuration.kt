package cmu.pasta.fray.core

import cmu.pasta.fray.core.logger.CsvLogger
import cmu.pasta.fray.core.logger.JsonLogger
import cmu.pasta.fray.core.logger.LoggerBase
import cmu.pasta.fray.core.scheduler.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.defaultByName
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import java.net.URI
import java.net.URLClassLoader
import java.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ExecutionInfo(
    val clazz: String,
    val method: String,
    val args: List<String>,
    val classpaths: List<String>
) {}

sealed class ExecutionConfig(name: String) : OptionGroup(name) {
  open fun getExecutionInfo(): ExecutionInfo {
    return ExecutionInfo("", "", emptyList(), emptyList())
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

  override fun getExecutionInfo(): ExecutionInfo {
    return ExecutionInfo(clazz, method, targetArgs, classpaths)
  }
}

class JsonExecutionConfig : ExecutionConfig("json") {
  val path by option("--config-path").file().required()

  override fun getExecutionInfo(): ExecutionInfo {
    return Json.decodeFromString<ExecutionInfo>(path.readText())
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
  val fullSchedule by option("-f", "--full").boolean().default(false)
  val logger by
      option("-l", "--logger").groupChoice("json" to JsonLoggerOption(), "csv" to CsvLoggerOption())
  val interleaveMemoryOps by option("-m", "--memory").boolean().default(false)
  val maxScheduledStep by option("-s", "--max-scheduled-step").int().default(10000)
  val ignoreUnhandledExceptions by
      option("-e", "--ignore-unhandled-exceptions").boolean().default(false)
  val scheduler by
      option()
          .groupChoice(
              "replay" to Replay(),
              "fifo" to Fifo(),
              "pos" to POS(),
              "random" to Rand(),
              "pct" to PCT())
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
    val exec = {
      val classLoader =
          URLClassLoader(
              executionInfo.classpaths.map { it -> URI("file://$it").toURL() }.toTypedArray(),
              Thread.currentThread().contextClassLoader)
      Thread.currentThread().contextClassLoader = classLoader
      val clazz = Class.forName(executionInfo.clazz, true, classLoader)
      if (executionInfo.args.isEmpty() && executionInfo.method != "main") {
        val m = clazz.getMethod(executionInfo.method)
        if (m.modifiers and java.lang.reflect.Modifier.STATIC == 0) {
          val obj = clazz.getConstructor().newInstance()
          m.invoke(obj)
        } else {
          m.invoke(null)
        }
      } else {
        val m = clazz.getMethod(executionInfo.method, Array<String>::class.java)
        m.invoke(null, executionInfo.args.toTypedArray())
      }
      Unit
    }
    return Configuration(
        exec,
        report,
        iter,
        scheduler!!.getScheduler(),
        fullSchedule,
        logger!!.getLogger(report, fullSchedule),
        interleaveMemoryOps,
        ignoreUnhandledExceptions,
        maxScheduledStep)
  }
}

data class Configuration(
    val exec: () -> Unit,
    val report: String,
    val iter: Int,
    val scheduler: Scheduler,
    val fullSchedule: Boolean,
    val logger: LoggerBase,
    val interleaveMemoryOps: Boolean,
    val ignoreUnhandledExceptions: Boolean,
    val maxScheduledStep: Int,
) {}
