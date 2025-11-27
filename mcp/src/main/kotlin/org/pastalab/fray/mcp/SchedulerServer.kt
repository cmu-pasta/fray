package org.pastalab.fray.mcp

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.GetPromptResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.PromptArgument
import io.modelcontextprotocol.kotlin.sdk.PromptMessage
import io.modelcontextprotocol.kotlin.sdk.Role
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.pastalab.fray.rmi.ThreadInfo
import org.pastalab.fray.rmi.ThreadState

class SchedulerServer(
    val scheduleResultListener: List<ScheduleResultListener>,
    val replayMode: Boolean
) {

  private fun CallToolRequest.getIntArg(name: String): Int? {
    return arguments[name]?.jsonPrimitive?.int
  }

  private fun missingArgError(argName: String): CallToolResult {
    return CallToolResult(content = listOf(TextContent("Missing $argName argument.")))
  }

  var allThreads = listOf<ThreadInfo>()
  var waitLatch: CountDownLatch? = null
  var finished = true
  var scheduled: ThreadInfo? = null
  var bugFound: Throwable? = null
  var serverThread =
      Thread {
            try {
              createStdIOServer()
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }
          .apply {
            isDaemon = true
            name = "Fray-MCP-Server"
            start()
          }

  fun configureServer(): Server {
    val server =
        Server(
            Implementation(name = "Fray mcp server", version = "0.1.0"),
            ServerOptions(
                capabilities =
                    ServerCapabilities(
                        prompts = ServerCapabilities.Prompts(listChanged = true),
                        resources =
                            ServerCapabilities.Resources(subscribe = true, listChanged = true),
                        tools = ServerCapabilities.Tools(listChanged = true),
                    )))
    server.addTool(
        name = "get_all_threads",
        description = "Get all threads that have been created in the SUT.",
    ) { request ->
      if (finished) {
        CallToolResult(
            content =
                listOf(
                    TextContent(
                        "The program has completed or has not yet started. Please run it again to explore more schedules.")),
        )
      } else {
        CallToolResult(content = allThreads.map { TextContent(it.toString()) })
      }
    }

    val (commandName, commandDescription, commandInput) =
        if (!replayMode) {
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

    if (replayMode) {} else {
      server.addPrompt(
          "find_concurrency_bug",
          "Let the LLM control the thread execution of the SUT to find concurrency bugs.",
          listOf(
              PromptArgument(
                  name = "Main class name",
                  description = "Main class of the program under test.",
                  required = true,
              ),
          ),
      ) { result ->
        GetPromptResult(
            messages =
                listOf(
                    PromptMessage(
                        role = Role.user,
                        content =
                            TextContent(
                                "In this task, you need to help to find concurrency bugs in the given program by scheduling the threads. " +
                                    "The main class of the program is ${result.arguments?.get("Main class name")}.\n\n" +
                                    "You are going to use the controlled concurrency testing framework Fray to schedule the threads of the program " +
                                    "execution. You can use the tool 'get_all_threads' to get all threads and their states. Then you can use the " +
                                    "tool 'run_thread' to pick one thread to run. Your goal is to find uncaught exceptions, deadlocks, or assertion " +
                                    "failures in the program execution. The source code of the program is provided in the current folder. You can " +
                                    "also use JDB to inspect the program state if needed with command `jdb -attach 5005`. Fray controls the concurrency " +
                                    "by instrumenting the Java bytecode of the program and inserting additional locks and concurrency primitives to force " +
                                    "the program to run in sequential order. Thus, if you inspect the stack trace of the program, you may see " +
                                    "additional frames related to Fray's instrumentation. "),
                    ),
                ),
            description = "The prompt for finding concurrency bugs in MCP mode.",
        )
      }
    }

    server.addTool(
        name = commandName, description = commandDescription, inputSchema = commandInput) { request
          ->
          if (finished) {
            return@addTool CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "The program has completed or has not yet started. Please run it again to explore more schedules.")),
            )
          }
          if (!replayMode) {
            processInputInExploreMode(request)?.let {
              return@addTool it
            }
          }
          val scheduledThread = scheduled!!
          schedule(scheduledThread)

          if (finished) {
            val msg =
                if (bugFound != null)
                    "A bug has been found.\n Exception: $bugFound\n stacktrace: ${bugFound!!.stackTraceToString()}. You may exit now."
                else
                    "No bug has been found. You may restart the program execution to explore more schedules."
            CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "Thread ${scheduledThread.threadIndex} is successfully scheduled. The program has finished. $msg"),
                    ))
          } else {
            CallToolResult(
                content =
                    listOf(
                        TextContent(
                            "Thread ${scheduledThread.threadIndex} is successfully schedule. The latest state of the schedule thread is shown below."),
                    ) +
                        allThreads
                            .filter { it.threadIndex == scheduledThread.threadIndex }
                            .map { TextContent(it.toString()) },
            )
          }
        }
    return server
  }

  fun processInputInExploreMode(request: CallToolRequest): CallToolResult? {
    val threadId = request.getIntArg("thread_id") ?: return missingArgError("thread_id")

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
    return null
  }

  fun schedule(thread: ThreadInfo) {
    scheduleResultListener.forEach { it.scheduled(thread) }
    waitLatch = CountDownLatch(1)
    try {
      waitLatch?.await()
    } catch (e: InterruptedException) {} finally {
      waitLatch = null
    }
  }

  fun newSchedulingRequestReceived(threads: List<ThreadInfo>, scheduled: ThreadInfo?) {
    allThreads = threads
    this.scheduled = scheduled
    waitLatch?.countDown()
  }

  fun stop() {
    serverThread.interrupt()
    waitLatch = null
  }

  fun createStdIOServer() {
    val server = configureServer()
    val transport =
        StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered(),
        )

    runBlocking {
      server.createSession(transport)
      val done = Job()
      server.onClose { done.complete() }
      done.join()
    }
  }

  fun onExecutionStart() {
    finished = false
    bugFound = null
  }

  fun onExecutionDone(bugFound: Throwable?) {
    finished = true
    this.bugFound = bugFound
    waitLatch?.countDown()
  }
}
