package org.pastalab.fray.idea.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JSplitPane
import org.pastalab.fray.idea.debugger.FrayScheduleObserver
import org.pastalab.fray.idea.getPsiFile
import org.pastalab.fray.idea.mcp.SchedulerMcpApi
import org.pastalab.fray.idea.objects.ThreadExecutionContext
import org.pastalab.fray.rmi.ThreadState

class FrayDebugPanel(val project: Project, val scheduleObserver: FrayScheduleObserver) : JPanel() {
  // UI Components
  private val controlPanel: SchedulerControlPanel
  private val threadTimelinePanel: ThreadTimelinePanel
  private val threadResourcePanel: ThreadResourcePanel

  // Data management
  private val threadInfoUpdaters: MutableMap<Editor, ThreadInfoUpdater> = mutableMapOf()
  var selected: ThreadExecutionContext? = null
  private var callback: ((ThreadExecutionContext) -> Unit)? = null
  val highlightManager = MultiThreadHighlightManager()

  init {
    layout = BorderLayout()

    // Create the control panel (thread selection, stack trace, etc.)
    controlPanel =
        SchedulerControlPanel(
            project,
            onThreadSelected = { threadInfo -> selected = threadInfo },
            onScheduleButtonPressed = { selectedThread -> scheduleButtonPressed(selectedThread) })

    // Create the thread timeline panel
    threadTimelinePanel = ThreadTimelinePanel()
    //    scheduleObserver.observers.add(threadTimelinePanel)

    // Create the thread resource panel
    threadResourcePanel = ThreadResourcePanel()

    // Create a main split pane for control panel and the right side
    val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPanel, createRightPanel())
    mainSplitPane.resizeWeight = 0.4 // Control panel gets 40% of width
    mainSplitPane.dividerLocation = 350 // Starting position

    // Add the split pane to the main layout
    add(mainSplitPane, BorderLayout.CENTER)
  }

  private val mcpApi = SchedulerMcpApi(project, controlPanel)

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

  fun scheduleButtonPressed(newSelected: ThreadExecutionContext?) {
    ApplicationManager.getApplication().invokeAndWait { highlightManager.clearAll() }
    threadInfoUpdaters.forEach { it.value.threadNameMapping.clear() }
    threadInfoUpdaters.clear()
    controlPanel.clear()
    threadResourcePanel.clear()

    if (newSelected != null) {
      threadTimelinePanel.onNewSchedule(newSelected)
      selected = newSelected
      callback?.invoke(newSelected) // Notify callback with selected thread ID
    }
  }

  fun stop() {
    mcpApi.stop()
    highlightManager.clearAll()
    threadInfoUpdaters.forEach { it.value.stop() }
    threadInfoUpdaters.clear()
    controlPanel.clear()
    threadResourcePanel.clear()
    //    scheduleObserver.observers.remove(threadTimelinePanel)
  }

  fun schedule(
      threads: List<ThreadExecutionContext>,
      onThreadSelected: (ThreadExecutionContext) -> Unit
  ) {
    mcpApi.updateThreadStatus(threads)
    processThreadsForHighlighting(threads)

    // We need to update the control panel after highlighting because
    // highlighting changes the opened editors, and we need to switch back.
    controlPanel.updateThreads(threads, selected)

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

  companion object {
    const val CONTENT_ID = "fray-scheduler"
  }
}
