package org.pastalab.fray.core.utils

class HelperThread : Thread("fray-helper-thread") {
  var shouldStop = false
  var currentRunnable: Runnable? = null

  init {
    isDaemon = true
  }

  override fun run() {
    while (!shouldStop) {
      val job: Runnable?
      synchronized(this) {
        while (currentRunnable == null && !shouldStop) {
          (this as Object).wait()
        }
        job = currentRunnable
        currentRunnable = null
      }
      try {
        job?.run()
      } catch (e: Throwable) {
        e.printStackTrace()
      }
    }
  }

  fun submit(runnable: Runnable) {
    synchronized(this) {
      Utils.verifyOrReport({ currentRunnable == null }, "Helper thread is already running a job")
      currentRunnable = runnable
      (this as Object).notify()
    }
  }

  fun stopHelperThread() {
    synchronized(this) {
      shouldStop = true
      (this as Object).notify()
    }
  }
}
