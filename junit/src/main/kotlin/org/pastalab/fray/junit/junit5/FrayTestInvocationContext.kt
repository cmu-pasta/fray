package org.pastalab.fray.junit.junit5

import org.junit.jupiter.api.extension.Extension
import org.junit.jupiter.api.extension.TestTemplateInvocationContext

class FrayTestInvocationContext : TestTemplateInvocationContext {
  override fun getAdditionalExtensions(): MutableList<Extension> {
    return listOf(FrayTestRunExtension()).toMutableList()
  }
}
