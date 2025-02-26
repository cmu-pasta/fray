package org.pastalab.fray.idea.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JPanel
import javax.swing.JSplitPane
import org.pastalab.fray.idea.objects.ThreadExecutionContext
import org.pastalab.fray.idea.ui.Colors.THREAD_DISABLED_COLOR
import org.pastalab.fray.rmi.ThreadState

class FrayDebugPanel(val project: Project) : JPanel() {
  // UI Components
  private val controlPanel: SchedulerControlPanel
  private val threadTimelinePanel: ThreadTimelinePanel

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
            onThreadSelected = { threadInfo ->
              selected = threadInfo
              threadTimelinePanel.repaint() // Refresh timeline to show selection
            },
            onScheduleButtonPressed = { selectedThread -> scheduleButtonPressed(selectedThread) })

    // Create the thread timeline panel
    threadTimelinePanel = ThreadTimelinePanel()

    // Create a split pane to hold both panels
    val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPanel, threadTimelinePanel)
    splitPane.resizeWeight = 0.5 // Equal distribution initially
    splitPane.dividerLocation = 350 // Starting position

    // Add the split pane to the main layout
    add(splitPane, BorderLayout.CENTER)
  }

  fun scheduleButtonPressed(newSelected: ThreadExecutionContext?) {
    ApplicationManager.getApplication().invokeAndWait { highlightManager.clearAll() }
    threadInfoUpdaters.forEach { it.value.threadNameMapping.clear() }
    threadInfoUpdaters.clear()
    controlPanel.clear()

    if (newSelected != null) {
      threadTimelinePanel.newThreadScheduled(newSelected)
      selected = newSelected
      callback?.invoke(newSelected) // Notify callback with selected thread ID
    }
  }

  fun stop() {
    highlightManager.clearAll()
    threadInfoUpdaters.forEach { it.value.stop() }
    threadInfoUpdaters.clear()
    controlPanel.clear()
  }

  fun schedule(
      enabledThreads: List<ThreadExecutionContext>,
      onThreadSelected: (ThreadExecutionContext) -> Unit
  ) {
    // Update the thread information in the control panel
    controlPanel.updateThreads(enabledThreads, selected)

    // Process thread information for editor highlighting
    processThreadsForHighlighting(enabledThreads)

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

      val start = document.getLineStartOffset(threadExecutionContext.executingLine - 1)
      val end = document.getLineEndOffset(threadExecutionContext.executingLine - 1)
      val color =
          if (threadExecutionContext.threadInfo.state == ThreadState.Blocked) THREAD_DISABLED_COLOR
          else Colors.getThreadColor(threadExecutionContext.threadInfo.index)

      val highlightAttributes =
          TextAttributes(
              null, // foreground color
              color, // background color
              null, // effect color
              null, // effect type
              Font.PLAIN,
          )

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
