package org.pastalab.fray.mcp

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import java.util.concurrent.CountDownLatch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.pastalab.fray.rmi.ThreadInfo
import org.pastalab.fray.rmi.ThreadState

class SchedulerMcpExplorer(
    classSourceProvider: ClassSourceProvider,
    val schedulerDelegate: SchedulerDelegate,
    val replayMode: Boolean
) : SchedulerMcpBase(classSourceProvider) {

  override fun configureServer(): Server {
    val server = super.configureServer()

    val (commandName, commandDescription, commandInput) =
        if (replayMode) {
          Triple(
              "run_thread",
              "Pick one thread to run.",
              Tool.Input(
                  properties =
                      JsonObject(
                          mapOf(
                              "thread_id" to
                                  JsonObject(
                                      mapOf(
                                          "type" to JsonPrimitive("number"),
                                          "description" to
                                              JsonPrimitive("The ID of the thread to run."),
                                      )))),
                  required = listOf("thread_id")))
        } else {
          Triple("next_step", "Run next scheduled thread.", Tool.Input())
        }

    server.addTool(
        name = commandName, description = commandDescription, inputSchema = commandInput) { request
          ->
          if (!replayMode) {
            processInputInExploreMode(request)?.let {
              return@addTool it
            }
          }

          if (finished) {
            val msg =
                if (bugFound != null) "A bug has been found.\n Exception stack trace: $bugFound"
                else "No bug has been found."
            CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "Thread ${scheduled?.threadIndex} is successfully scheduled. The program has finished. $msg"),
                    ))
          } else {
            CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "Thread ${scheduled?.threadIndex} is successfully schedule. New thread states are shown below."),
                    ) + allThreads.map { TextContent(it.toString()) },
            )
          }
        }
    return server
  }

  fun processInputInExploreMode(request: CallToolRequest): CallToolResult? {
    val threadId = request.arguments["thread_id"]?.jsonPrimitive?.int
    if (threadId == null) {
      return CallToolResult(
          content = listOf(TextContent("Missing thread_id argument.")),
      )
    }
    val thread = allThreads.find { it.threadIndex == threadId }
    if (thread == null) {
      return CallToolResult(
          content = listOf(TextContent("No thread found with ID $threadId.")),
      )
    }
    if (thread.state != ThreadState.Runnable) {
      return CallToolResult(
          content = listOf(TextContent("The selected thread is not runnable.")),
      )
    }
    scheduled = thread
    scheduleAndWait(thread)
    return null
  }

  fun scheduleAndWait(thread: ThreadInfo) {
    waitLatch = CountDownLatch(1)
    schedulerDelegate.scheduled(thread)
    try {
      waitLatch?.await()
    } catch (e: InterruptedException) {}
    waitLatch = null
  }
}
