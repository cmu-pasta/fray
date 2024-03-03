package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.runtime.Runtime
import cmu.pasta.sfuzz.core.concurrency.logger.JsonLogger
import cmu.pasta.sfuzz.runtime.TargetTerminateException
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.jvm.Throws

fun runProgram(className: String, reportPath: String, targetArgs: String) {
    println("Start analysing $className:main")
    println("Report is available at: $reportPath")
    prepareReportPath(reportPath)
    val clazz = Class.forName(className)
    val m = clazz.getMethod("main", Array<String>::class.java)
    val logger = JsonLogger(reportPath)
    GlobalContext.registerLogger(logger)
    GlobalContext.start()
    Runtime.DELEGATE = RuntimeDelegate()

    try {
        m.invoke(null, targetArgs.split(" ").toTypedArray())
    } catch (e: TargetTerminateException) {
        println("target terminated: ${e.status}")
    } catch (e: Throwable) {
        e.printStackTrace()
    }

    GlobalContext.done()
    logger.dump()
    println("Analysis done!")
}

@OptIn(ExperimentalPathApi::class)
fun prepareReportPath(reportPath: String) {
    val path = Paths.get(reportPath)
    path.deleteRecursively()
    path.createDirectories()
}

fun main(args: Array<String>) {
    val parser = ArgParser("sfuzz")
    val clazz by parser.argument(ArgType.String, description = "Class that contains main method")
    val report by parser.option(ArgType.String, shortName = "o", description = "Report location").default("report")
    val targetArgs by parser.option(ArgType.String, shortName =  "a", description = "Arguments to the application").default("")
    parser.parse(args)

    runProgram(clazz, report, targetArgs)
}