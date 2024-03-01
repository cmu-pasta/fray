package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.concurrency.logger.JsonLogger
import cmu.pasta.sfuzz.runtime.Runtime
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

fun runProgram(className: String, reportPath: String) {
    prepareReportPath(reportPath)

    val clazz = Class.forName(className)
    val m = clazz.getMethod("main", Array<String>::class.java)
    val logger = JsonLogger(reportPath)
    GlobalContext.registerLogger(logger)
    GlobalContext.start()
    Runtime.DELEGATE = RuntimeDelegate()

    m.invoke(null, emptyArray<String>())
    GlobalContext.done()

    logger.dump()
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
    parser.parse(args)

    runProgram(clazz, report)
}