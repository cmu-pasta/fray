package org.pastalab.fray.idea.ui

import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


class SchedulerPanel: JPanel() {
  val placeholderLabel: JLabel
  private val comboBox: JComboBox<Long>
  private val scheduleButton: JButton
  private var callback: ((Long) -> Unit)? = null // Callback to notify when a thread is selected

  init {
    layout = BorderLayout()
    val text = "Schedulable threads will be shown here once available."
    placeholderLabel = JLabel(text, SwingConstants.CENTER)
    add(placeholderLabel, BorderLayout.CENTER)

    comboBox = JComboBox<Long>()
    comboBox.isVisible = false
    add(comboBox, BorderLayout.NORTH)

    // Button to confirm selection
    scheduleButton = JButton("Schedule")
    scheduleButton.isVisible = false
    scheduleButton.addActionListener {
      val selectedId = comboBox.selectedItem as? Long
      if (selectedId != null) {
        callback?.invoke(selectedId) // Notify callback with selected thread ID
      }
    }
    add(scheduleButton, BorderLayout.SOUTH)
  }

  fun schedule(enabledIds: List<Long>, onThreadSelected: (Long) -> Unit) {
    if (enabledIds.isEmpty()) {
      // If no threads are available, show placeholder text
      placeholderLabel.isVisible = true
      comboBox.isVisible = false
      scheduleButton.isVisible = false
    } else {
      // Update comboBox with available thread IDs
      comboBox.removeAllItems()
      enabledIds.forEach { comboBox.addItem(it) }
      comboBox.isVisible = true
      scheduleButton.isVisible = true
      placeholderLabel.isVisible = false
    }
    callback = onThreadSelected
  }
}
