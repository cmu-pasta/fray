package mcp

import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
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
import kotlinx.serialization.json.jsonPrimitive
import org.pastalab.fray.rmi.ThreadInfo

open class SchedulerMcpBase(val classSourceProvider: ClassSourceProvider) {

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

  open fun configureServer(): Server {
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
      val className = request.arguments["class_name"]?.jsonPrimitive?.content
      if (className == null) {
        return@addTool CallToolResult(
            content = listOf(TextContent("Missing class_name argument.")),
        )
      }
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

    return server
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
