package org.pastalab.fray.idea.ui

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JPanel
import javax.swing.ToolTipManager
import org.pastalab.fray.idea.objects.ThreadExecutionContext
import org.pastalab.fray.rmi.ScheduleObserver

data class ThreadExecutionHistory(
    var threadName: String,
    val events: MutableList<Pair<Int, String>>
)

class ThreadTimelinePanel : JPanel(){
  private val threadExecutionHistory = mutableMapOf<Int, ThreadExecutionHistory>()
  private val timelineCanvas = ThreadTimelineCanvas()
  private var currentTime = 0

  init {
    layout = BorderLayout()
    val scrollPane = JBScrollPane(timelineCanvas)
    scrollPane.preferredSize = Dimension(300, 0) // Default width for timeline
    add(scrollPane, BorderLayout.CENTER)

    // Configure tooltip for faster display
    ToolTipManager.sharedInstance().initialDelay = 100
    ToolTipManager.sharedInstance().dismissDelay = 5000
  }

  fun newThreadScheduled(thread: ThreadExecutionContext) {
    val frame = thread.executingFrame ?: return
    val history =
        threadExecutionHistory.getOrPut(thread.threadInfo.threadIndex) {
          ThreadExecutionHistory(thread.threadInfo.threadName, mutableListOf())
        }
    history.threadName = thread.threadInfo.threadName
    history.events.add(Pair(currentTime++, frame.toString()))
    // Request repaint to show the updated timeline
    timelineCanvas.repaint()
  }

  fun onNewSchedule(
      scheduled: ThreadExecutionContext
  ) {
    newThreadScheduled(scheduled)
  }

  inner class ThreadTimelineCanvas : JPanel() {
    private val rowHeight = 30
    private val threadNameWidth = 100
    private val eventWidth = 8
    private val eventHeight = 16
    private val eventRadius = 4

    private var hoveredEvent: Pair<Int, Pair<Int, String>>? = null

    init {
      // Set a preferred size to make panel scrollable
      preferredSize = Dimension(800, 500)

      // Add mouse listeners for interaction
      addMouseMotionListener(
          object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
              // Detect if mouse is over an event marker
              val x = e.x
              val y = e.y

              // Clear previous hover state
              val previousHoveredEvent = hoveredEvent
              hoveredEvent = null

              // Check each thread's events
              for ((threadIndex, history) in threadExecutionHistory) {
                val threadY = threadIndex.toInt() * rowHeight + rowHeight / 2

                // Check if mouse is in this thread's row
                if (y >= threadY - eventHeight / 2 && y <= threadY + eventHeight / 2) {
                  // Check each event
                  for (event in history.events) {
                    val eventX = threadNameWidth + (event.first * 20)

                    // Check if mouse is over this event
                    if (x >= eventX - eventWidth / 2 && x <= eventX + eventWidth / 2) {
                      hoveredEvent = Pair(threadIndex, event)
                      break
                    }
                  }
                }
              }

              // Repaint if hover state changed
              if (previousHoveredEvent != hoveredEvent) {
                repaint()
              }
            }
          })

      // Add tooltip support
      toolTipText = " "
    }

    override fun getToolTipText(event: MouseEvent): String? {
      if (hoveredEvent != null) {
        val (threadIndex, eventData) = hoveredEvent!!
        val (timeStep, frameInfo) = eventData
        return "Thread $threadIndex - Step $timeStep:\n$frameInfo"
      }
      return null
    }

    override fun getPreferredSize(): Dimension {
      val height = maxOf(500, (threadExecutionHistory.size + 1) * rowHeight)
      val maxTimeStep =
          threadExecutionHistory.values.flatMap { it.events }.maxOfOrNull { it.first } ?: 0
      val width = maxOf(800, threadNameWidth + (maxTimeStep + 1) * 20 + 100)
      return Dimension(width, height)
    }

    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2d = g as Graphics2D
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

      // Draw background
      g2d.color = background
      g2d.fillRect(0, 0, width, height)

      // Draw vertical separator for thread names
      g2d.color = JBColor.border()
      g2d.drawLine(threadNameWidth, 0, threadNameWidth, height)

      // Draw horizontal lines for each thread
      for (i in 0..threadExecutionHistory.size) {
        val y = i * rowHeight
        g2d.color = JBColor.border().brighter()
        g2d.drawLine(0, y, width, y)
      }

      threadExecutionHistory.entries.forEachIndexed { index, entry ->
        val threadIndex = entry.key
        val history = entry.value
        val y = index * rowHeight + rowHeight / 2

        // Draw thread name
        g2d.color = JBColor.foreground()
        g2d.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        g2d.drawString(history.threadName, 10, y + 5)

        // Draw timeline for this thread
        drawThreadTimeline(g2d, threadIndex, history.events, y)
      }
    }

    private fun drawThreadTimeline(
        g2d: Graphics2D,
        threadIndex: Int,
        events: List<Pair<Int, String>>,
        y: Int
    ) {
      // Create a color for this thread (you can use more sophisticated color mapping)
      val threadColor = Colors.getThreadColor(threadIndex)

      // Draw connecting line for this thread's events
      if (events.size > 1) {
        g2d.color = threadColor.darker()
        g2d.stroke = BasicStroke(1.5f)

        val points = events.map { threadNameWidth + (it.first * 20) }
        for (i in 0 until points.size - 1) {
          g2d.drawLine(points[i], y, points[i + 1], y)
        }
      }

      // Draw each execution event
      events.forEachIndexed { index, event ->
        val timeStep = event.first
        val x = threadNameWidth + (timeStep * 20)

        // Draw event marker
        val isHovered =
            hoveredEvent?.let { it.first == threadIndex && it.second.first == timeStep } == true

        if (isHovered) {
          // Draw highlighted event
          g2d.color = JBColor.YELLOW
          g2d.fillOval(x - eventWidth / 2, y - eventHeight / 2, eventWidth, eventHeight)
          g2d.color = threadColor.darker()
          g2d.stroke = BasicStroke(2f)
          g2d.drawOval(x - eventWidth / 2, y - eventHeight / 2, eventWidth, eventHeight)
        } else {
          // Draw normal event
          g2d.color = threadColor
          g2d.fillOval(x - eventRadius, y - eventRadius, eventRadius * 2, eventRadius * 2)
          g2d.color = threadColor.darker()
          g2d.stroke = BasicStroke(1f)
          g2d.drawOval(x - eventRadius, y - eventRadius, eventRadius * 2, eventRadius * 2)
        }

        g2d.color = JBColor.foreground()
        g2d.font = Font(Font.MONOSPACED, Font.PLAIN, 9)
        g2d.drawString("${index+1}", x - 3, y + eventHeight)
      }
    }
  }
}
