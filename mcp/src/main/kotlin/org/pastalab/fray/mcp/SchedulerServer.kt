package org.pastalab.fray.mcp

import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import java.util.concurrent.CountDownLatch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.pastalab.fray.rmi.ThreadInfo
import org.pastalab.fray.rmi.ThreadState

class SchedulerServer(
    val classSourceProvider: ClassSourceProvider,
    val schedulerDelegate: SchedulerDelegate,
    val debuggerProvider: DebuggerProvider,
    val replayMode: Boolean
) {

  // Utility functions to simplify argument extraction
  private fun CallToolRequest.getStringArg(name: String): String? {
    val value = arguments[name]?.jsonPrimitive?.content
    if (value == null) {
      return null
    }
    return value
  }

  private fun CallToolRequest.getIntArg(name: String): Int? {
    return arguments[name]?.jsonPrimitive?.int
  }

  private fun missingArgError(argName: String): CallToolResult {
    return CallToolResult(content = listOf(TextContent("Missing $argName argument.")))
  }

  var allThreads = listOf<ThreadInfo>()
  var waitLatch: CountDownLatch? = null
  var finished = false
  var scheduled: ThreadInfo? = null
  var bugFound: Throwable? = null
  val embeddedServer = createEmbeddedServer(8808)
  var serverThread =
      Thread {
            try {
              embeddedServer.start(wait = true)
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
        description = "Get all threads that has been created in the SUT.",
    ) { request ->
      CallToolResult(content = allThreads.map { TextContent(it.toString()) })
    }

    server.addTool(
        name = "get_source",
        description = "Get source file for a given class name.",
        inputSchema =
            Tool.Input(
                properties =
                    JsonObject(
                        mapOf(
                            "class_name" to
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("string"),
                                        "description" to
                                            JsonPrimitive(
                                                "The class name to get the source file for."),
                                    )))),
                required = listOf("class_name")),
    ) { request ->
      val className =
          request.getStringArg("class_name") ?: return@addTool missingArgError("class_name")

      val source = classSourceProvider.getClassSource(className)
      if (source == null) {
        return@addTool CallToolResult(
            content = listOf(TextContent("No source file found for class $className.")),
        )
      }
      CallToolResult(
          content = listOf(TextContent(source)),
      )
    }

    server.addTool(
        name = "get_variable_value",
        description = "Get the value of a variable in a given method.",
        inputSchema =
            Tool.Input(
                properties =
                    JsonObject(
                        mapOf(
                            "thread_id" to
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("number"),
                                        "description" to JsonPrimitive("The ID of the thread."),
                                    )),
                            "class_name" to
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("string"),
                                        "description" to
                                            JsonPrimitive("The full class name of the method."),
                                    )),
                            "method_name" to
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("string"),
                                        "description" to JsonPrimitive("The name of the method."),
                                    )),
                            "line_number" to
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("number"),
                                        "description" to
                                            JsonPrimitive("The line number of the method."),
                                    )),
                            "field_name" to
                                JsonObject(
                                    mapOf(
                                        "type" to JsonPrimitive("string"),
                                        "description" to
                                            JsonPrimitive("The field name of the variable."),
                                    )),
                        )),
                required =
                    listOf("thread_id", "class_name", "method_name", "line_number", "field_name"),
            )) { request ->
          // Extract all arguments with simplified code
          val threadId =
              request.getIntArg("thread_id") ?: return@addTool missingArgError("thread_id")
          val jvmThreadId =
              allThreads.firstOrNull { it.threadIndex == threadId }?.jvmThreadIndex
                  ?: return@addTool CallToolResult(
                      content = listOf(TextContent("The thread with ID $threadId is not found.")),
                  )

          val className =
              request.getStringArg("class_name") ?: return@addTool missingArgError("class_name")
          val methodName =
              request.getStringArg("method_name") ?: return@addTool missingArgError("method_name")
          val lineNumber =
              request.getIntArg("line_number") ?: return@addTool missingArgError("line_number")
          val fieldName =
              request.getStringArg("field_name") ?: return@addTool missingArgError("field_name")
          val field = request.getStringArg("field")

          val result =
              debuggerProvider.getLocalVariableValue(
                  jvmThreadId, className, methodName, lineNumber, fieldName, field)

          return@addTool CallToolResult(
              content =
                  listOf(
                      TextContent(
                          result.fold(
                              onSuccess = { it },
                              onFailure = { "Error retrieving variable: ${it.message}" }))))
        }

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

  fun newSchedulingRequestReceived(threads: List<ThreadInfo>, scheduled: ThreadInfo?) {
    allThreads = threads
    this.scheduled = scheduled
    waitLatch?.countDown()
  }

  fun stop() {
    embeddedServer.stop(1000, 2000)
    serverThread.interrupt()
    waitLatch = null
  }

  fun createEmbeddedServer(
      port: Int
  ): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> {
    return embeddedServer(CIO, host = "0.0.0.0", port = port) {
      mcp {
        return@mcp configureServer()
      }
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
