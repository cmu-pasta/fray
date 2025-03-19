package org.anonlab.fray.idea

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls

object FrayBundle {
  @NonNls val BUNDLE = "messages.FrayBundle"

  val INSTANCE = DynamicBundle(FrayBundle::class.java, BUNDLE)
}
