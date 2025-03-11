package org.pastalab.fray.idea.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.pom.Navigatable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import org.pastalab.fray.idea.getPsiFile
import org.pastalab.fray.idea.objects.ThreadExecutionContext
import org.pastalab.fray.rmi.ThreadState

/** Panel that contains the thread selector, stack trace viewer, and scheduling controls. */
class SchedulerControlPanel(
    val project: Project,
    private val onThreadSelected: (ThreadExecutionContext) -> Unit,
    private val onScheduleButtonPressed: (ThreadExecutionContext?) -> Unit
) : JPanel() {
  // UI Components
  private val comboBoxModel = DefaultComboBoxModel<ThreadExecutionContext>()
  private val comboBox: ComboBox<ThreadExecutionContext>
  private val myFrameList: JBList<StackTraceElement>
  private val myFrameListModel: DefaultListModel<StackTraceElement>
  private val scheduleButton: JButton

  // Current thread selection
  var selectedThread: ThreadExecutionContext? = null

  init {
    layout = BorderLayout()
    preferredSize = Dimension(350, 0)

    // Thread selector combo box
    comboBox = ComboBox<ThreadExecutionContext>(comboBoxModel)
    comboBox.renderer =
        object : DefaultListCellRenderer() {
          override fun getListCellRendererComponent(
              list: JList<*>?,
              value: Any?,
              index: Int,
              isSelected: Boolean,
              cellHasFocus: Boolean
          ): Component {
            val label =
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    as JLabel
            if (value is ThreadExecutionContext) {
              label.text = "Thread: $value"
            }
            return label
          }
        }
    comboBox.addActionListener {
      val selected = comboBox.selectedItem as? ThreadExecutionContext
      if (selected != null) {
        handleThreadSelection(selected)
      }
    }
    add(comboBox, BorderLayout.NORTH)

    // Stack trace viewer
    myFrameListModel = DefaultListModel()
    myFrameList = JBList(myFrameListModel)
    myFrameList.cellRenderer = ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
      JBLabel("\t\t$value")
    }
    myFrameList.addMouseListener(
        object : MouseAdapter() {
          override fun mouseClicked(e: MouseEvent) {
            if (e.clickCount == 2) {
              val selectedFrame = myFrameList.selectedValue
              if (selectedFrame != null) {
                navigateToSource(selectedFrame)
              }
            }
          }
        })
    val scrollPane = JBScrollPane(myFrameList)
    add(scrollPane, BorderLayout.CENTER)

    // Schedule button
    scheduleButton = JButton("Schedule")
    scheduleButton.addActionListener { onScheduleButtonPressed(selectedThread) }
    add(scheduleButton, BorderLayout.SOUTH)
  }

  /** Handles selection of a thread from the combo box or externally */
  fun handleThreadSelection(context: ThreadExecutionContext) {
    selectedThread = context

    for (st in context.threadInfo.stackTraces) {
      if (navigateToSource(st)) {
        break
      }
    }
    // Update the stack trace view
    myFrameListModel.clear()
    ApplicationManager.getApplication().invokeAndWait {
      context.threadInfo.stackTraces.forEach { myFrameListModel.addElement(it) }
      scheduleButton.isEnabled = context.threadInfo.state == ThreadState.Runnable
    }

    // Notify the parent component of the selection
    onThreadSelected(context)
  }

  /** Updates the panel with new thread information */
  fun updateThreads(
      threads: List<ThreadExecutionContext>,
      previouslySelected: ThreadExecutionContext? = null
  ) {
    // Update the combo box model
    comboBoxModel.removeAllElements()
    comboBoxModel.addAll(threads)

    // Try to restore previous selection or select the first thread
    if (threads.isNotEmpty()) {
      val threadToSelect =
          threads.find { it.threadInfo.threadIndex == previouslySelected?.threadInfo?.threadIndex }
              ?: threads.first()
      comboBoxModel.selectedItem = threadToSelect
    }
  }

  /** Clears all thread data */
  fun clear() {
    comboBoxModel.removeAllElements()
    myFrameListModel.clear()
    selectedThread = null
  }

  /** Navigates to the source code location of the selected stack frame */
  private fun navigateToSource(frame: StackTraceElement): Boolean {
    val psiFile = frame.getPsiFile(project) ?: return false
    val virtualFile = psiFile.virtualFile
    val editorManager = FileEditorManager.getInstance(project)
    ApplicationManager.getApplication().invokeLater {
      val editor = editorManager.openFile(virtualFile, true).firstOrNull()
      if (editor is TextEditor) {
        val lineNumber = frame.lineNumber - 1
        if (lineNumber >= 0 && lineNumber < editor.editor.document.lineCount) {
          editor.editor.caretModel.moveToLogicalPosition(LogicalPosition(lineNumber, 0))
          editor.editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
        }
      }
    }
    return true
  }
}
