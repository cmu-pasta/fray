package cmu.pasta.sfuzz.core.concurrency.logger

import cmu.pasta.sfuzz.core.concurrency.operations.Operation
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class JsonLogger(val base: String): LoggerBase {
    val executions = mutableListOf<MutableList<Operation>>()
    val json = Json {
        prettyPrint = true
    }
    override fun executionStart() {
        executions.add(mutableListOf())
    }
    override fun newOperationScheduled(op: Operation) {
        executions.last().add(op)
    }
    override fun executionDone() {
    }

    fun dump() {
        File("$base/timeline.json").writeText(json.encodeToString(executions))
    }
}