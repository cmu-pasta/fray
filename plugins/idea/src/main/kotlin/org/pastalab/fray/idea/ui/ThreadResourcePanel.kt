package org.pastalab.fray.idea.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.*
import javax.swing.*
import kotlin.toString
import org.pastalab.fray.idea.objects.ThreadExecutionContext

class ThreadResourcePanel : JPanel() {
  private var threadInfos = emptyList<ThreadExecutionContext>()
  private val resourcePanel = JPanel()

  init {
    layout = BorderLayout()
    resourcePanel.layout = BoxLayout(resourcePanel, BoxLayout.Y_AXIS)
    val scrollPane = JBScrollPane(resourcePanel)
    scrollPane.border = JBUI.Borders.empty()
    add(scrollPane, BorderLayout.CENTER)
  }

  fun updateThreadResources(threadInfos: List<ThreadExecutionContext>) {
    this.threadInfos = threadInfos
    updatePanel()
  }

  fun clear() {
    updatePanel()
  }

  private fun updatePanel() {
    resourcePanel.removeAll()

    if (threadInfos.isEmpty()) {
      val emptyLabel = JLabel("No thread resource information available", SwingConstants.CENTER)
      emptyLabel.foreground = JBColor.GRAY
      resourcePanel.add(emptyLabel)
    } else {
      var first = true

      for (threadInfo in threadInfos) {
        if (!first) {
          resourcePanel.add(JSeparator(SwingConstants.HORIZONTAL))
        } else {
          first = false
        }

        val threadItem = createThreadItem(threadInfo)
        resourcePanel.add(threadItem)
      }
    }

    revalidate()
    repaint()
  }

  private fun createResourceItem(icon: Icon, text: String): JPanel {
    val waitPanel = JPanel(BorderLayout(5, 0))
    val waitIconLabel = JLabel(icon)
    waitIconLabel.border = JBUI.Borders.emptyRight(5)
    waitPanel.add(waitIconLabel, BorderLayout.WEST)
    waitPanel.add(JLabel(text), BorderLayout.CENTER)
    return waitPanel
  }

  private fun createThreadItem(threadInfo: ThreadExecutionContext): JPanel {
    val panel = JPanel(BorderLayout(10, 5))
    panel.border = JBUI.Borders.empty(10, 5)

    // Thread header panel with icon
    val headerPanel = JPanel(BorderLayout(5, 0))
    val icon = threadInfo.threadStateIcon()
    val iconLabel = JLabel(icon)
    iconLabel.border = JBUI.Borders.emptyRight(5)
    headerPanel.add(iconLabel, BorderLayout.WEST)

    val threadStateText = threadInfo.threadInfo.state.toString()
    val threadNameLabel =
        JLabel("<html><b>${threadInfo.threadInfo.threadName}</b> ($threadStateText)</html>")
    headerPanel.add(threadNameLabel, BorderLayout.CENTER)
    panel.add(headerPanel, BorderLayout.NORTH)

    // Resources panel
    val resourcesPanel = JPanel(GridBagLayout())
    resourcesPanel.border = JBUI.Borders.empty(5, 20, 0, 0)

    val constraints =
        GridBagConstraints().apply {
          gridx = 0
          gridy = 0
          anchor = GridBagConstraints.WEST
          fill = GridBagConstraints.HORIZONTAL
          weightx = 1.0
          insets = JBUI.insets(2, 0)
        }

    if (threadInfo.threadInfo.acquired.isNotEmpty()) {
      val titleLabel = JLabel("<html><b>Acquired locks:</b></html>")
      resourcesPanel.add(titleLabel, constraints)
      constraints.gridy++

      for (lock in threadInfo.threadInfo.acquired) {
        constraints.insets = JBUI.insets(2, 0) // Reset insets
        resourcesPanel.add(
            createResourceItem(
                IconLoader.getIcon("/icons/locked.svg", this::class.java),
                lock.toString(),
            ),
            constraints,
        )
        constraints.gridy++
      }
    }

    if (threadInfo.threadInfo.waitingFor != null) {
      constraints.insets = JBUI.insets(6, 0, 2, 0) // Add extra space above
      val titleLabel = JLabel("<html><b>Waiting for:</b></html>")
      resourcesPanel.add(titleLabel, constraints)
      constraints.gridy++

      constraints.insets = JBUI.insets(2, 0) // Reset insets
      resourcesPanel.add(
          createResourceItem(
              AllIcons.Debugger.ThreadFrozen,
              threadInfo.threadInfo.waitingFor.toString(),
          ),
          constraints,
      )
      constraints.gridy++
    }

    if (threadInfo.threadInfo.acquired.isEmpty() && threadInfo.threadInfo.waitingFor == null) {
      resourcesPanel.add(JLabel("No resources acquired or waiting for"), constraints)
    }

    panel.add(resourcesPanel, BorderLayout.CENTER)
    return panel
  }
}
