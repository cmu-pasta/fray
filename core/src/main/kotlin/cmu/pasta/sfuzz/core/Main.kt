package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.logger.ConsoleLogger
import cmu.pasta.sfuzz.core.logger.CsvLogger
import cmu.pasta.sfuzz.runtime.Runtime
import cmu.pasta.sfuzz.core.logger.JsonLogger
import cmu.pasta.sfuzz.core.runtime.AnalysisResult
import cmu.pasta.sfuzz.runtime.Delegate
import cmu.pasta.sfuzz.runtime.TargetTerminateException
import java.lang.reflect.InvocationTargetException
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
fun prepareReportPath(reportPath: String) {
    val path = Paths.get(reportPath)
    path.deleteRecursively()
    path.createDirectories()
}

fun run(config: Configuration) {
    println("Start analysing ${config.clazz}:main")
    println("Report is available at: ${config.report}")
    prepareReportPath(config.report)
    val logger = JsonLogger(config.report, config.fullSchedule)
    GlobalContext.registerLogger(logger)
//    GlobalContext.registerLogger(ConsoleLogger())
    GlobalContext.scheduler = config.scheduler
    GlobalContext.config = config
    GlobalContext.bootstrap()
    for (i in 0..<config.iter) {
        try {
            Runtime.DELEGATE = RuntimeDelegate()
            Runtime.start()
            val clazz = Class.forName(config.clazz)
            if (config.targetArgs.isEmpty() && config.method != "main") {
                val m = clazz.getMethod(config.method)
                m.invoke(null)
            } else {
                val m = clazz.getMethod(config.method, Array<String>::class.java)
                m.invoke(null, config.targetArgs.split(" ").toTypedArray())
            }
            Runtime.onMainExit()
        } catch (e: InvocationTargetException) {
            if (e.cause is TargetTerminateException) {
                Runtime.onMainExit()
                println("target terminated: ${(e.cause as TargetTerminateException).status}")
            } else {
                println(e.toString())
                e.cause?.printStackTrace()
            }
        }
        Runtime.DELEGATE = Delegate()
        GlobalContext.done(AnalysisResult.COMPLETE)
    }
    GlobalContext.shutDown()
    logger.dump()
    println("Analysis done!")
}


fun main(args: Array<String>) {
    val config = ConfigurationCommand().apply { main(args) }.toConfiguration()
    run(config)
}
