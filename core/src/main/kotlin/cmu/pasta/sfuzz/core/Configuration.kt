package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.scheduler.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.serialization.json.Json

sealed class ScheduleAlgorithm(name: String): OptionGroup(name) {
    open fun getScheduler(): Scheduler {
        return FifoScheduler()
    }
}

class Replay: ScheduleAlgorithm("replay") {
    val path by option().file().required()

    override fun getScheduler(): Scheduler {
        return ReplayScheduler(path.readText())
    }
}

class Fifo: ScheduleAlgorithm("fifo") {
    override fun getScheduler(): Scheduler {
        return FifoScheduler()
    }
}

class PCT: ScheduleAlgorithm("pct") {
    val numSwitchPoints by option().int().default(3)
}

class ConfigurationCommand: CliktCommand() {
    val clazz by argument()
    val report by option("-o").default("report")
    val targetArgs by option("-a", "--args", help = "Arguments passed to target application").default("")
    val scheduler by option().groupChoice(
        "replay" to Replay(),
        "fifo" to Fifo()
    )
    val fullSchedule by option("-f", "--full").boolean().default(false)

    override fun run() {
    }


    fun toConfiguration(): Configuration {
        return Configuration(clazz, report, targetArgs, scheduler!!.getScheduler(), fullSchedule)
    }
}

data class Configuration(val clazz: String, val report: String, val targetArgs: String, val scheduler: Scheduler, val fullSchedule: Boolean) {
}