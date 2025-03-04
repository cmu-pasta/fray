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
    scheduleObserver.observers.add(threadTimelinePanel)

    // Create the thread resource panel
    threadResourcePanel = ThreadResourcePanel()

    // Create a main split pane for control panel and the right side
    val mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPanel, createRightPanel())
    mainSplitPane.resizeWeight = 0.4 // Control panel gets 40% of width
    mainSplitPane.dividerLocation = 350 // Starting position

    // Add the split pane to the main layout
    add(mainSplitPane, BorderLayout.CENTER)
  }

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
      selected = newSelected
      callback?.invoke(newSelected) // Notify callback with selected thread ID
    }
  }

  fun stop() {
    highlightManager.clearAll()
    threadInfoUpdaters.forEach { it.value.stop() }
    threadInfoUpdaters.clear()
    controlPanel.clear()
    threadResourcePanel.clear()
    scheduleObserver.observers.remove(threadTimelinePanel)
  }

  fun schedule(
      enabledThreads: List<ThreadExecutionContext>,
      onThreadSelected: (ThreadExecutionContext) -> Unit
  ) {
    // Update the thread information in the control panel
    controlPanel.updateThreads(enabledThreads, selected)

    // Process thread information for editor highlighting
    processThreadsForHighlighting(enabledThreads)

    // Update thread resource information
    threadResourcePanel.updateThreadResources(enabledThreads)

    // Store the callback
    callback = onThreadSelected
  }

  /** Process thread information and add editor highlighting */
  private fun processThreadsForHighlighting(threads: List<ThreadExecutionContext>) {
    threads.forEach { threadExecutionContext ->
      if (threadExecutionContext.threadInfo.state == ThreadState.Completed) return@forEach
      if (threadExecutionContext.executingLine < 0) return@forEach
      val document = threadExecutionContext.document ?: return@forEach
      val vFile = threadExecutionContext.virtualFile ?: return@forEach
      ApplicationManager.getApplication().invokeLater {
        FileEditorManager.getInstance(project).openFile(vFile).forEach { fileEditor ->
          if (fileEditor is TextEditor) {
            val editor = fileEditor.editor
            highlightManager.addThreadToLine(
                threadExecutionContext.executingLine,
                threadExecutionContext,
                editor,
                document,
                project)
            threadInfoUpdaters
                .getOrPut(editor) {
                  val updater = ThreadInfoUpdater(editor)
                  editor.addEditorMouseMotionListener(updater)
                  updater
                }
                .threadNameMapping
                .getOrPut(threadExecutionContext.executingLine - 1) { mutableSetOf() }
                .add(threadExecutionContext)
          }
        }
      }
    }
  }

  companion object {
    const val CONTENT_ID = "fray-scheduler"
  }
}
