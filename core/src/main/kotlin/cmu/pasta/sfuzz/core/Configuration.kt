package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.logger.CsvLogger
import cmu.pasta.sfuzz.core.logger.JsonLogger
import cmu.pasta.sfuzz.core.logger.LoggerBase
import cmu.pasta.sfuzz.core.scheduler.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import java.util.*

sealed class Logger(name: String) : OptionGroup(name) {
  open fun getLogger(baseFolder: String, fullSchedule: Boolean): LoggerBase {
    return JsonLogger(baseFolder, fullSchedule)
  }
}

class Json : Logger("json") {
  override fun getLogger(baseFolder: String, fullSchedule: Boolean): LoggerBase {
    return JsonLogger(baseFolder, fullSchedule)
  }
}

class Csv : Logger("csv") {
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
    return ReplayScheduler(Schedule.fromString(path.readText(), path.extension == "json"))
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
}

class ConfigurationCommand : CliktCommand() {
  val clazz by argument()
  val method by argument()
  val report by option("-o").default("/tmp/report")
  val targetArgs by
      option("-a", "--args", help = "Arguments passed to target application").default("")
  val iter by option("-i", "--iter", help = "Number of iterations").int().default(1)
  val scheduler by
      option()
          .groupChoice("replay" to Replay(), "fifo" to Fifo(), "pos" to POS(), "random" to Rand())
  val fullSchedule by option("-f", "--full").boolean().default(false)
  val logger by option("-l", "--logger").groupChoice("json" to Json(), "csv" to Csv())

  override fun run() {}

  fun toConfiguration(): Configuration {
    val exec = {
      val clazz = Class.forName(clazz)
      if (targetArgs.isEmpty() && method != "main") {
        val m = clazz.getMethod(method)
        m.invoke(null)
      } else {
        val m = clazz.getMethod(method, Array<String>::class.java)
        m.invoke(null, targetArgs.split(" ").toTypedArray())
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
    )
  }
}

data class Configuration(
    val exec: () -> Unit,
    val report: String,
    val iter: Int,
    val scheduler: Scheduler,
    val fullSchedule: Boolean,
    val logger: LoggerBase
) {}
