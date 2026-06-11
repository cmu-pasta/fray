package org.pastalab.fray.idea.ui

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.rd.util.ConcurrentHashMap
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JPanel
import javax.swing.ToolTipManager
import kotlin.math.abs
import org.pastalab.fray.idea.objects.ThreadExecutionContext

data class ThreadExecutionHistory(
    var threadName: String,
    val events: MutableList<Pair<Int, String>>,
)

class ThreadTimelinePanel : JPanel() {
  val threadExecutionHistory = ConcurrentHashMap<Int, ThreadExecutionHistory>()
  private val timelineCanvas = ThreadTimelineCanvas()
  private var currentTime = 0
  private val scrollPane = JBScrollPane(timelineCanvas)

  init {
    layout = BorderLayout()
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

  fun onNewSchedule(allThreads: List<ThreadExecutionContext>, scheduled: ThreadExecutionContext) {
    if (allThreads.size > 1 || threadExecutionHistory.isEmpty()) {
      newThreadScheduled(scheduled)
    }
  }

  inner class ThreadTimelineCanvas : JPanel() {
    private val rowHeight = JBUI.scale(30)
    private val eventWidth = JBUI.scale(8)
    private val eventHeight = JBUI.scale(16)
    private val eventRadius = JBUI.scale(4)
    private val eventSpacing = JBUI.scale(20)
    private val padding = JBUI.scale(10)

    private var hoveredEvent: Pair<Int, Pair<Int, String>>? = null
    private var separatorPosition = JBUI.scale(105)

    private val separatorDragTolerance = JBUI.scale(5)
    private var isDraggingSeparator = false

    init {
      // Set a preferred size to make panel scrollable
      preferredSize = Dimension(JBUI.scale(800), JBUI.scale(500))

      addMouseListener(
          object : java.awt.event.MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
              // Check if click is near the separator
              if (abs(e.x - separatorPosition) <= separatorDragTolerance) {
                isDraggingSeparator = true
                setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR))
              }
            }

            override fun mouseReleased(e: MouseEvent) {
              isDraggingSeparator = false
              setCursor(java.awt.Cursor.getDefaultCursor())
            }
          }
      )

      // Add mouse listeners for interaction
      addMouseMotionListener(
          object : MouseMotionAdapter() {

            override fun mouseDragged(e: MouseEvent) {
              if (isDraggingSeparator) {
                val minWidth = JBUI.scale(50)
                val newPosition = maxOf(minWidth, e.x)
                updateSeparatorPosition(newPosition)
                repaint()
              }
            }

            override fun mouseMoved(e: MouseEvent) {
              val x = e.x
              val y = e.y

              val previousHoveredEvent = hoveredEvent
              hoveredEvent = null

              for ((threadIndex, history) in threadExecutionHistory) {
                val threadY =
                    threadExecutionHistory.keys.indexOf(threadIndex) * rowHeight + rowHeight / 2

                if (y >= threadY - eventHeight && y <= threadY + eventHeight) {
                  for (event in history.events) {
                    val eventX = separatorPosition + (event.first * eventSpacing)

                    if (x >= eventX - eventWidth && x <= eventX + eventWidth) {
                      hoveredEvent = Pair(threadIndex, event)
                      break
                    }
                  }
                }
              }

              if (previousHoveredEvent != hoveredEvent) {
                repaint()
                ToolTipManager.sharedInstance()
                    .mouseMoved(
                        MouseEvent(
                            this@ThreadTimelineCanvas,
                            MouseEvent.MOUSE_MOVED,
                            System.currentTimeMillis(),
                            0,
                            x,
                            y,
                            0,
                            false,
                        )
                    )
              }

              if (abs(e.x - separatorPosition) <= separatorDragTolerance) {
                setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR))
              } else {
                setCursor(java.awt.Cursor.getDefaultCursor())
              }
            }
          }
      )

      // Enable tooltips
      ToolTipManager.sharedInstance().registerComponent(this)
    }

    fun updateSeparatorPosition(newPosition: Int) {
      separatorPosition = newPosition
      revalidate()
      repaint()
    }

    override fun getToolTipText(event: MouseEvent): String? {
      return hoveredEvent?.let { (_, eventData) ->
        val (timeStep, frameInfo) = eventData
        "Step: ${timeStep + 1}\n$frameInfo"
      }
    }

    override fun getPreferredSize(): Dimension {
      val height = maxOf(JBUI.scale(500), (threadExecutionHistory.size + 1) * rowHeight)
      val maxTimeStep =
          threadExecutionHistory.values.flatMap { it.events }.maxOfOrNull { it.first } ?: 0
      val width =
          maxOf(
              JBUI.scale(800),
              separatorPosition + (maxTimeStep + 1) * eventSpacing + JBUI.scale(100),
          )
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
      g2d.drawLine(separatorPosition, 0, separatorPosition, height)

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
        g2d.font = JBUI.Fonts.create(JBFont.MONOSPACED, JBUI.scaleFontSize(12.0f))
        val metrics = g2d.fontMetrics
        val maxChars = (separatorPosition - padding * 2) / metrics.charWidth('m')
        val threadName =
            if (history.threadName.length > maxChars) {
              history.threadName.substring(0, maxChars) + "..."
            } else {
              history.threadName
            }
        g2d.drawString(threadName, padding, y + metrics.height / 2 - 1)

        // Draw timeline for this thread, copy list to avoid concurrent modification.
        drawThreadTimeline(g2d, threadIndex, history.events.toList(), y)
      }
    }

    private fun drawThreadTimeline(
        g2d: Graphics2D,
        threadIndex: Int,
        events: List<Pair<Int, String>>,
        y: Int,
    ) {
      // Create a color for this thread (you can use more sophisticated color mapping)
      val threadColor = Colors.getThreadColor(threadIndex)

      // Draw connecting line for this thread's events
      if (events.size > 1) {
        g2d.color = threadColor.darker()
        g2d.stroke = BasicStroke(JBUIScale.scale(1.5f))

        val points = events.map { separatorPosition + (it.first * eventSpacing) }
        for (i in 0 until points.size - 1) {
          g2d.drawLine(points[i], y, points[i + 1], y)
        }
      }

      // Draw each execution event
      events.forEachIndexed { index, event ->
        val timeStep = event.first
        val x = separatorPosition + (timeStep * eventSpacing)

        // Draw event marker
        val isHovered =
            hoveredEvent?.let { it.first == threadIndex && it.second.first == timeStep } == true

        if (isHovered) {
          // Draw highlighted event
          g2d.color = JBColor.YELLOW
          g2d.fillOval(x - eventWidth / 2, y - eventHeight / 2, eventWidth, eventHeight)
          g2d.color = threadColor.darker()
          g2d.stroke = BasicStroke(JBUIScale.scale(2f))
          g2d.drawOval(x - eventWidth / 2, y - eventHeight / 2, eventWidth, eventHeight)
        } else {
          // Draw normal event
          g2d.color = threadColor
          g2d.fillOval(x - eventRadius, y - eventRadius, eventRadius * 2, eventRadius * 2)
          g2d.color = threadColor.darker()
          g2d.stroke = BasicStroke(JBUIScale.scale(1f))
          g2d.drawOval(x - eventRadius, y - eventRadius, eventRadius * 2, eventRadius * 2)
        }

        g2d.color = JBColor.foreground()
        g2d.font = UIUtil.getFont(UIUtil.FontSize.SMALL, UIUtil.getLabelFont())
        g2d.drawString("${index+1}", x - 3, y + eventHeight)
      }
    }
  }
}
