package org.anonlab.fray.junit.junit5

import org.anonlab.fray.core.RunContext
import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.TestTemplateInvocationContext

class FrayTestInvocationContext(
    val index: Int,
    val displayName: String,
    val context: RunContext,
    val frayJupiterContext: FrayJupiterContext
) : TestTemplateInvocationContext {
  override fun getDisplayName(invocationIndex: Int): String {
    return "$displayName repetition ${context.config.currentIteration} of ${context.config.iter}"
  }

  override fun getAdditionalExtensions(): MutableList<Extension> {
    return listOf(FrayExtension(index, context, frayJupiterContext)).toMutableList()
  }
}
