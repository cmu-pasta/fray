package org.pastalab.fray.idea.ui

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.EffectType
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

class MultiThreadHighlightManager {
  // Map to track which threads are highlighting which lines
  private val lineThreadsMap = mutableMapOf<Int, MutableSet<ThreadExecutionContext>>()

  // Map to track highlighters by line
  private val lineHighlightersMap = mutableMapOf<Int, MutableMap<Editor, RangeHighlighter>>()

  // Add a thread to a line and update highlighting
  fun addThreadToLine(
      line: Int,
      threadContext: ThreadExecutionContext,
      editor: Editor,
      document: Document,
      project: Project
  ): RangeHighlighter {
    // Add thread to the line
    val threadsForLine = lineThreadsMap.getOrPut(line) { mutableSetOf() }
    threadsForLine.add(threadContext)

    // Get existing highlighter for this line in this editor, or null if none exists
    val existingHighlighter = lineHighlightersMap.getOrPut(line) { mutableMapOf() }.get(editor)

    // If an existing highlighter exists, remove it first
    if (existingHighlighter != null) {
      editor.markupModel.removeHighlighter(existingHighlighter)
    }

    // Create a new highlighter with appropriate visual appearance
    val start = document.getLineStartOffset(line - 1)
    val end = document.getLineEndOffset(line - 1)

    // Create appropriate visual attributes based on threads on this line
    val attributes = createMultiThreadAttributes(threadsForLine)

    // Add the highlighter
    val highlighter =
        editor.markupModel.addRangeHighlighter(
            start, end, 0, attributes, HighlighterTargetArea.LINES_IN_RANGE)

    // Store the highlighter
    lineHighlightersMap.getOrPut(line) { mutableMapOf() }[editor] = highlighter

    return highlighter
  }

  // Remove a thread from a line and update highlighting
  fun removeThreadFromLine(line: Int, threadContext: ThreadExecutionContext, editor: Editor) {
    // Get threads for this line
    val threadsForLine = lineThreadsMap[line] ?: return

    // Remove this thread
    threadsForLine.remove(threadContext)

    // Get existing highlighter
    val existingHighlighter =
        lineHighlightersMap.getOrPut(line) { mutableMapOf() }.get(editor) ?: return

    // If no threads left, remove the highlighter
    if (threadsForLine.isEmpty()) {
      editor.markupModel.removeHighlighter(existingHighlighter)
      lineHighlightersMap[line]?.remove(editor)
      lineThreadsMap.remove(line)
    } else {
      // Otherwise, update the highlighter with new appearance
      updateHighlighterAttributes(existingHighlighter, threadsForLine)
    }
  }

  // Clear all highlighting
  fun clearAll() {
    // For each line and editor, remove highlighters
    lineHighlightersMap.forEach { (line, editorMap) ->
      editorMap.forEach { (editor, highlighter) ->
        editor.markupModel.removeHighlighter(highlighter)
      }
    }

    // Clear maps
    lineHighlightersMap.clear()
    lineThreadsMap.clear()
  }

  // Create visual attributes for multi-thread highlighting
  private fun createMultiThreadAttributes(threads: Set<ThreadExecutionContext>): TextAttributes {
    if (threads.isEmpty()) {
      return TextAttributes()
    }

    if (threads.size == 1) {
      // Single thread - use its color
      val thread = threads.first()
      val color =
          if (thread.threadInfo.state == ThreadState.Paused) THREAD_DISABLED_COLOR
          else Colors.getThreadColor(thread.threadInfo.index)

      return TextAttributes(
          null, // foreground color
          color, // background color
          null, // effect color
          null, // effect type
          Font.PLAIN)
    } else {
      // Multiple threads - use special visualization

      // Option 1: Use a special "multi-thread" color
      val multiThreadColor = JBColor(Color(255, 230, 180), Color(100, 80, 30))

      // Option 2: Create striped/patterned effect (more advanced)
      // This requires custom rendering and is more complex

      // Option 3: Create gradient or blend of colors
      // val blendedColor = blendColors(threads.map { Colors.getThreadColor(it.threadInfo.index) })

      // For now, use the special color and add an effect
      return TextAttributes(
          null, // foreground color
          multiThreadColor, // background color
          JBColor.BLACK, // effect color
          EffectType.WAVE_UNDERSCORE, // add an underscore to indicate multiple threads
          Font.PLAIN)
    }
  }

  // Update highlighter attributes based on threads
  private fun updateHighlighterAttributes(
      highlighter: RangeHighlighter,
      threads: Set<ThreadExecutionContext>
  ) {
    //    highlighter.textAttributes = createMultiThreadAttributes(threads)
  }
}
