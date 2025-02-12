package org.pastalab.fray.idea.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import org.pastalab.fray.rmi.ThreadInfo
import org.pastalab.fray.rmi.ThreadState

class SchedulerPanel(val project: Project) : JPanel() {
  private val comboBoxModel = DefaultComboBoxModel<ThreadInfo>()
  private val comboBox: ComboBox<ThreadInfo>
  private val myFrameList: JBList<StackTraceElement>
  private val myFrameListModel: DefaultListModel<StackTraceElement>
  private val currentRangeHighlighters = mutableListOf<Pair<MarkupModel, RangeHighlighter>>()

  private val scheduleButton: JButton
  var selected: ThreadInfo? = null
  private var callback: ((ThreadInfo) -> Unit)? =
      null // Callback to notify when a thread is selected

  init {
    layout = BorderLayout()
    comboBox = ComboBox<ThreadInfo>(comboBoxModel)
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
            if (value is ThreadInfo) {
              label.text = "Thread-${value.threadName} (${value.state})"
            }
            return label
          }
        }
    comboBox.addActionListener {
      val selectedThread = comboBox.selectedItem as? ThreadInfo
      if (selectedThread != null) {
        comboBoxSelected(selectedThread)
      }
    }
    add(comboBox, BorderLayout.NORTH)

    // Table to display stack trace
    myFrameListModel = DefaultListModel()
    myFrameList = JBList(myFrameListModel)
    myFrameList.cellRenderer = ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
      JBLabel("\t\t$value")
    }
    val scrollPane = JBScrollPane(myFrameList)
    add(scrollPane, BorderLayout.CENTER)

    // Button to confirm selection
    scheduleButton = JButton("Schedule")
    scheduleButton.addActionListener { scheduleButtonPressed() }
    add(scheduleButton, BorderLayout.SOUTH)
  }

  fun scheduleButtonPressed() {
    val newSelected = comboBox.selectedItem as? ThreadInfo
    ApplicationManager.getApplication().invokeAndWait {
      comboBoxModel.removeAllElements()
      currentRangeHighlighters.forEach { it.first.removeHighlighter(it.second) }
    }
    if (newSelected != null) {
      selected = newSelected
      callback?.invoke(newSelected) // Notify callback with selected thread ID
    }
  }

  fun stop() {
    currentRangeHighlighters.forEach { it.first.removeHighlighter(it.second) }
  }

  fun comboBoxSelected(threadInfo: ThreadInfo) {
    myFrameListModel.clear()
    ApplicationManager.getApplication().invokeAndWait {
      threadInfo.stackTraces.forEach { myFrameListModel.addElement(it) }
      if (threadInfo.state == ThreadState.Enabled) {
        scheduleButton.isEnabled = true
      } else {
        scheduleButton.isEnabled = false
      }
    }
  }

  fun schedule(enabledThreads: List<ThreadInfo>, onThreadSelected: (ThreadInfo) -> Unit) {
    enabledThreads.forEach { threadInfo ->
      comboBoxModel.addElement(threadInfo)
      if (threadInfo.state == ThreadState.Completed) return@forEach
      for (stack in threadInfo.stackTraces) {
        if (stack.className == "ThreadStartOperation") continue
        if (runReadAction {
          val psiClass =
              JavaPsiFacade.getInstance(project)
                  .findClass(stack.className, GlobalSearchScope.projectScope(project))
                  ?: return@runReadAction false
          val psiFile = psiClass.containingFile
          val document = psiFile.fileDocument
          val start = document.getLineStartOffset(stack.lineNumber - 1)
          val end = document.getLineEndOffset(stack.lineNumber - 1)
          val color =
              if (threadInfo.state == ThreadState.Paused) JBColor.LIGHT_GRAY
              else JBColor(Color(228, 251, 233), Color(228, 251, 233))
          val highlightAttributes =
              TextAttributes(
                  null, // foreground color
                  color, // background color
                  null, // effect color
                  null, // effect type
                  Font.PLAIN,
              )
          val vFile = psiFile.virtualFile
          ApplicationManager.getApplication().invokeLater {
            FileEditorManager.getInstance(project).openFile(vFile).forEach { fileEditor ->
              if (fileEditor is TextEditor) {
                val highlighter =
                    fileEditor.editor.markupModel.addRangeHighlighter(
                        start,
                        end,
                        0,
                        highlightAttributes,
                        com.intellij.openapi.editor.markup.HighlighterTargetArea.LINES_IN_RANGE)
                highlighter.errorStripeTooltip =
                    "Thread-${threadInfo.threadName} (${threadInfo.state})"
                currentRangeHighlighters.add(Pair(fileEditor.editor.markupModel, highlighter))
              }
            }
          }
          true
        })
            break
      }
    }
    comboBoxModel.selectedItem =
        enabledThreads.find { it.index == selected?.index } ?: enabledThreads.first()
    callback = onThreadSelected
  }

  companion object {
    const val CONTENT_ID = "fray-scheduler"
  }
}
