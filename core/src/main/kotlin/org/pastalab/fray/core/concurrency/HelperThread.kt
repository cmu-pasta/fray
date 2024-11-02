package org.pastalab.fray.core.concurrency

open class HelperThread(runnable: Runnable?) : Thread(runnable) {
  constructor() : this(null)
}
