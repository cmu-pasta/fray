package org.pastalab.fray.idea.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeTooltipManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.HintHint
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.panels.NonOpaquePanel
import org.pastalab.fray.idea.`object`.ThreadExecutionContext
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JSeparator
import javax.swing.border.EmptyBorder
import org.pastalab.fray.idea.ui.Colors.THREAD_DISABLED_COLOR
import org.pastalab.fray.idea.ui.Colors.THREAD_ENABLED_COLOR
import org.pastalab.fray.rmi.ThreadState

class ThreadInfoUpdater(val editor: Editor) : EditorMouseMotionListener {
  val threadNameMapping: MutableMap<Int, MutableSet<ThreadExecutionContext>> = mutableMapOf()
  var threadInfoBalloon: Balloon? = null

  fun getBalloonLabelOf(offset: Int): String {
    val threadInfos = threadNameMapping[offset]
    if (threadInfos == null) {
      return ""
    }
    return threadInfos.joinToString("\n")
  }

  fun buildBalloonComponent(offset: Int): JComponent? {
    val threadInfos = threadNameMapping[offset]
    if (threadInfos == null) {
      return null
    }

    val label = JLabel()
    val iconTextGap = (label.iconTextGap * 1.5).toInt()
    val content = NonOpaquePanel(BorderLayout(iconTextGap, iconTextGap))
    content.layout = BoxLayout(content, BoxLayout.Y_AXIS)

    var first = true
    for (context in threadInfos) {
      val threadItem = NonOpaquePanel(BorderLayout())
      val (bgColor, icon) =
          if (context.threadInfo.state == ThreadState.Enabled)
              Pair(THREAD_ENABLED_COLOR, AllIcons.Debugger.ThreadRunning)
          else Pair(THREAD_DISABLED_COLOR, AllIcons.Debugger.ThreadFrozen)
      val text =
          IdeTooltipManager.initPane(
              context.toString(),
              HintHint().setTextFg(JBColor.BLACK).setTextBg(bgColor).setAwtTooltip(true),
              null)
      text.isEditable = false
      text.border = null
      val textWrapper = NonOpaquePanel(BorderLayout())
      textWrapper.add(text)
      textWrapper.background = bgColor
      val north = NonOpaquePanel(BorderLayout())
      val iconLabel = JLabel(icon)
      iconLabel.border = EmptyBorder(0, 0, 0, iconTextGap)
      north.add(iconLabel, BorderLayout.NORTH)
      threadItem.add(north, BorderLayout.WEST)
      threadItem.add(textWrapper, BorderLayout.CENTER)
      threadItem.background = bgColor
      threadItem.border = EmptyBorder(iconTextGap, 0, iconTextGap, 0)
      if (!first) {
        content.add(JSeparator(), BorderLayout.CENTER)
      } else {
        first = false
      }
      content.add(threadItem, BorderLayout.CENTER)
    }
    return content
  }

  override fun mouseMoved(e: EditorMouseEvent) {
    val offset = e.logicalPosition.line
    val label = getBalloonLabelOf(offset)
    if (label.isEmpty()) {
      return
    }
    if (threadInfoBalloon?.isDisposed != false) {
      threadInfoBalloon?.hide()
      val content = buildBalloonComponent(offset) ?: return

      val logicalPosition = LogicalPosition(offset, e.logicalPosition.column)
      val visualPosition = editor.logicalToVisualPosition(logicalPosition)
      val point = editor.visualPositionToXY(visualPosition)
      val relativePoint = RelativePoint(editor.contentComponent, point)

      threadInfoBalloon =
          JBPopupFactory.getInstance()
              .createBalloonBuilder(content)
              .setFillColor(JBColor.background())
              .createBalloon()
      threadInfoBalloon?.show(relativePoint, Balloon.Position.above)
    }
  }

  fun stop() {
    threadInfoBalloon?.hide()
    editor.removeEditorMouseMotionListener(this)
  }
}
