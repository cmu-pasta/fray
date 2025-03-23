package org.pastalab.fray.idea.mcp

import com.intellij.openapi.project.Project
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.server.MCP
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.pastalab.fray.idea.getPsiFileFromClass
import org.pastalab.fray.idea.objects.ThreadExecutionContext
import org.pastalab.fray.idea.ui.SchedulerControlPanel
import org.pastalab.fray.rmi.ScheduleObserver

open class SchedulerMcpBase(val project: Project, val schedulerPanel: SchedulerControlPanel) :
    ScheduleObserver<ThreadExecutionContext> {

  var allThreads = listOf<ThreadExecutionContext>()
  var waitLatch: CountDownLatch? = null
  var finished = false
  var bugFound = false
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
    val def = CompletableDeferred<Unit>()
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
                    )),
            onCloseCallback = { def.complete(Unit) })
    server.addTool(
        name = "get_all_threads",
        description = "Get all threads that has been created in the SUT.",
    ) { request ->
      CallToolResult(content = allThreads.map { TextContent(it.threadInfo.toString()) })
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
      val psiClass = getPsiFileFromClass(className, project)
      if (psiClass == null) {
        return@addTool CallToolResult(
            content = listOf(TextContent("No source file found for class $className.")),
        )
      }
      CallToolResult(
          content = listOf(TextContent(psiClass.text)),
      )
    }

    return server
  }

  fun newSchedulingRequestReceived(threads: List<ThreadExecutionContext>) {
    allThreads = threads
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
      MCP {
        return@MCP configureServer()
      }
    }
  }

  override fun onExecutionStart() {
    finished = false
    bugFound = false
  }

  override fun onNewSchedule(
      allThreads: List<ThreadExecutionContext>,
      scheduled: ThreadExecutionContext
  ) {}

  override fun onExecutionDone(bugFound: Boolean) {
    finished = true
    this.bugFound = bugFound
    waitLatch?.countDown()
  }

  override fun saveToReportFolder(path: String) {}
}
