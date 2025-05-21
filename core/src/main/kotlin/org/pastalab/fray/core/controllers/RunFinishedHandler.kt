package org.pastalab.fray.core.controllers

import org.pastalab.fray.core.RunContext

abstract class RunFinishedHandler(context: RunContext) {
  init {
    context.runFinishedHandlers.add(this)
  }

  abstract fun done()
}
