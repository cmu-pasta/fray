package org.pastalab.fray.idea.ui

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint

class ThreadInfoUpdater(val editor: Editor) : EditorMouseMotionListener {
  val threadNameMapping: MutableMap<Int, String> = mutableMapOf()
  var threadInfoBalloon: Balloon? = null
  var currentThreadName = ""

  override fun mouseMoved(e: EditorMouseEvent) {
    val offset = e.logicalPosition.line
    if (threadNameMapping.contains(offset) &&
        ((threadInfoBalloon?.isDisposed != false) ||
            currentThreadName != threadNameMapping[offset])) {
      threadInfoBalloon?.hide()

      val logicalPosition = LogicalPosition(offset, e.logicalPosition.column)
      val visualPosition = editor.logicalToVisualPosition(logicalPosition)
      val point = editor.visualPositionToXY(visualPosition)
      val relativePoint = RelativePoint(editor.contentComponent, point)

      threadInfoBalloon =
          JBPopupFactory.getInstance()
              .createHtmlTextBalloonBuilder("${threadNameMapping[offset]}", MessageType.INFO, null)
              .createBalloon()
      threadInfoBalloon?.show(relativePoint, Balloon.Position.above)
      currentThreadName = threadNameMapping[offset]!!
    }
  }

  fun stop() {
    threadInfoBalloon?.hide()
    editor.removeEditorMouseMotionListener(this)
  }
}
