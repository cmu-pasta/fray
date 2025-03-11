package org.pastalab.fray.idea.ui

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font
import org.pastalab.fray.idea.objects.ThreadExecutionContext
import org.pastalab.fray.idea.ui.Colors.THREAD_DISABLED_COLOR
import org.pastalab.fray.rmi.ThreadState
import com.intellij.openapi.editor.markup.HighlighterLayer

class MultiThreadHighlightManager {
  // Map to track highlighters by line
  private val lineHighlightersMap = mutableMapOf<Int, MutableMap<Editor, RangeHighlighter>>()

  private val THREAD_HIGHLIGHT_LAYER = HighlighterLayer.SELECTION + 100

  // Add a thread to a line and update highlighting
  fun addThreadToLine(
      line: Int,
      threadContext: ThreadExecutionContext,
      editor: Editor,
      document: Document,
      project: Project
  ): RangeHighlighter {
    val existingHighlighter = lineHighlightersMap.getOrPut(line) { mutableMapOf() }.get(editor)

    val start = document.getLineStartOffset(line - 1)
    val end = document.getLineEndOffset(line - 1)
    val attributes =
        if (existingHighlighter != null) {
          editor.markupModel.removeHighlighter(existingHighlighter)
          createMultiThreadAttributes()
        } else {
          createThreadAttributes(threadContext)
        }

    val highlighter =
        editor.markupModel.addRangeHighlighter(
            start, end, THREAD_HIGHLIGHT_LAYER, attributes, HighlighterTargetArea.LINES_IN_RANGE)

    lineHighlightersMap.getOrPut(line) { mutableMapOf() }[editor] = highlighter

    return highlighter
  }

  fun clearAll() {
    // For each line and editor, remove highlighters
    lineHighlightersMap.forEach { (line, editorMap) ->
      editorMap.forEach { (editor, highlighter) ->
        editor.markupModel.removeHighlighter(highlighter)
      }
    }

    // Clear maps
    lineHighlightersMap.clear()
  }

  private fun createThreadAttributes(thread: ThreadExecutionContext): TextAttributes {
    val color =
        if (thread.threadInfo.state == ThreadState.Blocked) THREAD_DISABLED_COLOR
        else Colors.getThreadColor(thread.threadInfo.threadIndex)

    return TextAttributes(
        null, // foreground color
        color, // background color
        null, // effect color
        null, // effect type
        Font.PLAIN)
  }

  // Create visual attributes for multi-thread highlighting
  private fun createMultiThreadAttributes(): TextAttributes {
    val multiThreadColor = JBColor(Color(255, 230, 180), Color(100, 80, 30))

    return TextAttributes(
        null, // foreground color
        multiThreadColor, // background color
        null, // effect color
        null,
        Font.PLAIN,
    )
  }
}
