package org.pastalab.fray.idea.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import javax.swing.SwingConstants
import org.pastalab.fray.rmi.ThreadInfo

class SchedulerPanel(val project: Project) : JPanel() {
  val placeholderLabel: JBLabel
  private val comboBoxModel = DefaultComboBoxModel<ThreadInfo>()
  private val comboBox: ComboBox<ThreadInfo>
  private val myFrameList: JBList<StackTraceElement>
  private val myFrameListModel: DefaultListModel<StackTraceElement>

  private val scheduleButton: JButton
  private var callback: ((ThreadInfo) -> Unit)? =
      null // Callback to notify when a thread is selected

  init {
    layout = BorderLayout()
    val text = "Schedulable threads will be shown here once available."
    placeholderLabel = JBLabel(text, SwingConstants.CENTER)
    add(placeholderLabel, BorderLayout.CENTER)

    comboBox = ComboBox<ThreadInfo>(comboBoxModel)
    comboBox.isVisible = false
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
              label.text = "Thread-${value.threadName}"
            }
            return label
          }
        }
    comboBox.addActionListener {
      val selectedThread = comboBox.selectedItem as? ThreadInfo
      if (selectedThread != null) {
        displayThreadInfo(selectedThread)
      }
    }
    add(comboBox, BorderLayout.NORTH)

    // Table to display stack trace
    myFrameListModel = DefaultListModel()
    myFrameList = JBList(myFrameListModel)
    myFrameList.cellRenderer =
        ListCellRenderer<StackTraceElement> { list, value, index, isSelected, cellHasFocus ->
          JBLabel("\t$value")
        }
    val scrollPane = JBScrollPane(myFrameList)
    add(scrollPane, BorderLayout.CENTER)

    // Button to confirm selection
    scheduleButton = JButton("Schedule")
    scheduleButton.isVisible = false
    scheduleButton.addActionListener {
      placeholderLabel.isVisible = true
      comboBox.isVisible = false
      scheduleButton.isVisible = false
      myFrameList.isVisible = false
      val selected = comboBox.selectedItem as? ThreadInfo
      if (selected != null) {
        callback?.invoke(selected) // Notify callback with selected thread ID
      }
    }
    add(scheduleButton, BorderLayout.SOUTH)
  }

  fun displayThreadInfo(threadInfo: ThreadInfo) {
    myFrameListModel.clear()
    threadInfo.stackTraces.forEach { myFrameListModel.addElement(it) }
  }

  fun schedule(enabledIds: List<ThreadInfo>, onThreadSelected: (ThreadInfo) -> Unit) {
    comboBoxModel.removeAllElements()
    enabledIds.forEach { comboBoxModel.addElement(it) }
    placeholderLabel.isVisible = false
    comboBox.isVisible = true
    scheduleButton.isVisible = true
    myFrameList.isVisible = true
    callback = onThreadSelected
  }

  companion object {
    const val CONTENT_ID = "fray-scheduler"
  }
}
