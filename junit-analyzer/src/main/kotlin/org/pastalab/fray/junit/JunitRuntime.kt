package org.pastalab.fray.junit

class JunitRuntimeDelegate : org.pastalab.fray.runtime.Delegate() {
  override fun onThreadStart(t: Thread?) {
    Recorder.logThread()
  }
}
