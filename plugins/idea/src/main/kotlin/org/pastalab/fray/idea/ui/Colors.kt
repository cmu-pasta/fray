package org.pastalab.fray.idea.ui

import com.intellij.ui.JBColor
import java.awt.Color

object Colors {
  val THREAD_ENABLED_COLOR = JBColor(Color(207, 243, 212), Color(43, 136, 55))
  val THREAD_DISABLED_COLOR = JBColor(Color(238, 238, 240), Color(196, 198, 198))

  fun getThreadColor(index: Int): JBColor {
    // Generate a consistent color based on thread index
    // Using HSB color model to create visually distinct colors
    val hue = ((index * 0.618033988749895) % 1).toFloat()

    // Create light and dark theme variants with reduced saturation
    val lightModeColor = Color.getHSBColor(hue, 0.35f, 0.85f)
    val darkModeColor = Color.getHSBColor(hue, 0.45f, 0.75f)

    // Return a JBColor that adapts to the current theme
    return JBColor(lightModeColor, darkModeColor)
  }
}
