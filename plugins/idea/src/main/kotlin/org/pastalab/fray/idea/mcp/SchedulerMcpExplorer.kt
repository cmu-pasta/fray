package org.pastalab.fray.idea.mcp

import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.string.printToString
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
import org.pastalab.fray.idea.objects.ThreadExecutionContext
import org.pastalab.fray.idea.ui.SchedulerControlPanel
import org.pastalab.fray.rmi.ThreadState

class SchedulerMcpExplorer(
    project: Project,
    schedulerPanel: SchedulerControlPanel,
    val replayMode: Boolean = false
) : SchedulerMcpBase(project, schedulerPanel) {

  override fun configureServer(): Server {
    val server = super.configureServer()

    val (command_name, command_description, command_input) =
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
        name = command_name, description = command_description, inputSchema = command_input) {
            request ->
          if (!replayMode) {
            processInputInExploreMode(request)?.let {
              return@addTool it
            }
          } else {

          }

          if (finished) {
            val msg =
                if (bugFound == null)
                    "A bug has been found.\n Exception stack trace: ${bugFound.printToString()}"
                else "No bug has been found."
            CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "Thread $threadId is successfully scheduled. The program has finished. $msg"),
                    ))
          } else {
            CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "Thread $threadId is successfully schedule. New thread states are shown below."),
                    ) + allThreads.map { TextContent(it.threadInfo.toString()) },
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
    val thread = allThreads.find { it.threadInfo.threadIndex == threadId }
    if (thread == null) {
      return CallToolResult(
          content = listOf(TextContent("No thread found with ID $threadId.")),
      )
    }
    if (thread.threadInfo.state != ThreadState.Runnable) {
      return CallToolResult(
          content = listOf(TextContent("The selected thread is not runnable.")),
      )
    }
    scheduleAndWait(thread)
    return null
  }

  fun scheduleAndWait(thread: ThreadExecutionContext) {
    waitLatch = CountDownLatch(1)
    schedulerPanel.comboBoxModel.selectedItem = thread
    schedulerPanel.onScheduleButtonPressed(thread)
    try {
      waitLatch?.await()
    } catch (e: InterruptedException) {}
    waitLatch = null
  }
}
