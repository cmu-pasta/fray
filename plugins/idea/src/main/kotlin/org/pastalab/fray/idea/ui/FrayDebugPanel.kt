package org.pastalab.fray.idea.ui

import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.debugger.jdi.VirtualMachineProxyImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugSession
import com.sun.jdi.ThreadReference
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSplitPane
import org.pastalab.fray.idea.getPsiFile
import org.pastalab.fray.idea.getPsiFileFromClass
import org.pastalab.fray.idea.objects.ThreadExecutionContext
import org.pastalab.fray.mcp.ClassSourceProvider
import org.pastalab.fray.mcp.DebuggerProvider
import org.pastalab.fray.mcp.RemoteVMConnector
import org.pastalab.fray.mcp.SchedulerDelegate
import org.pastalab.fray.mcp.SchedulerServer
import org.pastalab.fray.mcp.VirtualMachineProxy
import org.pastalab.fray.rmi.ScheduleObserver
import org.pastalab.fray.rmi.ThreadInfo
import org.pastalab.fray.rmi.ThreadState

class FrayDebugPanel(val debugSession: XDebugSession, replayMode: Boolean) :
    JPanel(), ScheduleObserver<ThreadExecutionContext> {
  private val project = debugSession.project
  // UI Components
  private val controlPanel: SchedulerControlPanel
  private val threadTimelinePanel: ThreadTimelinePanel
  private val threadResourcePanel: ThreadResourcePanel

  // Data management
  private val threadInfoUpdaters: MutableMap<Editor, ThreadInfoUpdater> = mutableMapOf()
  var selected: ThreadInfo? = null
  private var callback: ((ThreadInfo) -> Unit)? = null
  val highlightManager = MultiThreadHighlightManager()

  init {
    layout = BorderLayout()

    // Create the control panel (thread selection, stack trace, etc.)
    controlPanel =
        SchedulerControlPanel(
            project,
            onThreadSelected = { threadInfo -> selected = threadInfo },
            onScheduleButtonPressed = { selectedThread -> scheduleButtonPressed(selectedThread) },
            replayMode)

    // Create the thread timeline panel
    threadTimelinePanel = ThreadTimelinePanel()

    // Create the thread resource panel
    threadResourcePanel = ThreadResourcePanel()

    // Create a main split pane for control panel and the right side
    val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPanel, createRightPanel())
    mainSplitPane.resizeWeight = 0.4 // Control panel gets 40% of width
    mainSplitPane.dividerLocation = 350 // Starting position

    // Add the split pane to the main layout
    add(mainSplitPane, BorderLayout.CENTER)
  }

  private val mcpServer =
      SchedulerServer(
          object : ClassSourceProvider {
            override fun getClassSource(className: String): String? {
              return getPsiFileFromClass(className, project)?.text
            }
          },
          object : SchedulerDelegate {
            override fun scheduled(thread: ThreadInfo) {
              scheduleButtonPressed(thread)
            }
          },
          RemoteVMConnector(object: VirtualMachineProxy {
            override fun allThreads(): List<ThreadReference> {
              val process = debugSession.debugProcess
              val proxyImpl = if (process is JavaDebugProcess) {
                process.debuggerSession.process.virtualMachineProxy
              } else null
              return proxyImpl?.allThreads()?.map { it.threadReference } ?: emptyList()
            }
          }),
          replayMode)

  private fun createRightPanel(): JPanel {
    // Create a panel to hold timeline and resource panels
    val rightPanel = JPanel(BorderLayout())

    // Create a vertical split pane for timeline and resource panels
    val rightSplitPane =
        JSplitPane(JSplitPane.VERTICAL_SPLIT, threadTimelinePanel, threadResourcePanel)
    rightSplitPane.resizeWeight = 0.6 // Timeline gets 60% of height
    rightSplitPane.dividerLocation = 300 // Starting position

    rightPanel.add(rightSplitPane, BorderLayout.CENTER)
    return rightPanel
  }

  fun scheduleButtonPressed(newSelected: ThreadInfo?) {
    println(mcpServer.debuggerProvider.getLocalVariableValue(
        1,
        "BankAccountTest",
        "withdraw",
        16,
        "this",
        "balance"
    ))
    threadInfoUpdaters.forEach { it.value.threadNameMapping.clear() }
    threadInfoUpdaters.clear()
    controlPanel.clear()
    threadResourcePanel.clear()

    if (newSelected != null) {
      selected = newSelected
      callback?.invoke(newSelected) // Notify callback with selected thread ID
    }
  }

  fun stop() {
    mcpServer.stop()
    highlightManager.clearAll()
    threadInfoUpdaters.forEach { it.value.stop() }
    threadInfoUpdaters.clear()
    controlPanel.clear()
    threadResourcePanel.clear()
  }

  /**
   * Schedules the next operation for the given threads.
   *
   * @param threads The list of `ThreadExecutionContext` representing the threads to be scheduled.
   * @param scheduled The `ThreadExecutionContext` of the thread that is currently scheduled, `null`
   *   in explore mode.
   * @param onThreadSelected A callback function to be invoked when a thread is selected.
   */
  fun schedule(
      threads: List<ThreadExecutionContext>,
      scheduled: ThreadExecutionContext?,
      onThreadSelected: (ThreadInfo) -> Unit
  ) {
    mcpServer.newSchedulingRequestReceived(threads.map { it.threadInfo }, scheduled?.threadInfo)
    processThreadsForHighlighting(threads)

    // We need to update the control panel after highlighting because
    // highlighting changes the opened editors, and we need to switch back.
    controlPanel.updateThreads(
        threads, threads.firstOrNull { it.threadInfo.threadIndex == selected?.threadIndex })

    // Update thread resource information
    threadResourcePanel.updateThreadResources(threads)

    // Store the callback
    callback = onThreadSelected
  }

  /** Process thread information and add editor highlighting */
  private fun processThreadsForHighlighting(threads: List<ThreadExecutionContext>) {
    threads.forEach { threadExecutionContext ->
      if (threadExecutionContext.threadInfo.state == ThreadState.Completed) return@forEach
      threadExecutionContext.threadInfo.stackTraces.reversed().forEach { stackTraceElement ->
        val psiFile = stackTraceElement.getPsiFile(project) ?: return@forEach
        val document = psiFile.fileDocument
        val vFile = psiFile.virtualFile
        ApplicationManager.getApplication().invokeLater {
          FileEditorManager.getInstance(project).openFile(vFile, false).forEach { fileEditor ->
            if (fileEditor is TextEditor) {
              val editor = fileEditor.editor
              highlightManager.addThreadToLine(
                  stackTraceElement.lineNumber, threadExecutionContext, editor, document, project)
              threadInfoUpdaters
                  .getOrPut(editor) {
                    val updater = ThreadInfoUpdater(editor)
                    editor.addEditorMouseMotionListener(updater)
                    updater
                  }
                  .threadNameMapping
                  .getOrPut(stackTraceElement.lineNumber - 1) { mutableSetOf() }
                  .add(threadExecutionContext)
            }
          }
        }
      }
    }
  }

  override fun onExecutionStart() {
    mcpServer.onExecutionStart()
  }

  override fun onNewSchedule(
      allThreads: List<ThreadExecutionContext>,
      scheduled: ThreadExecutionContext
  ) {
    threadTimelinePanel.onNewSchedule(allThreads, scheduled)
  }

  override fun onExecutionDone(bugFound: Throwable?) {
    mcpServer.onExecutionDone(bugFound)
  }

  override fun saveToReportFolder(path: String) {}

  companion object {
    const val CONTENT_ID = "fray-scheduler"
  }
}
