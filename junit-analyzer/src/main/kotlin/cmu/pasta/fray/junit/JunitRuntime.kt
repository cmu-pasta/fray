package cmu.pasta.fray.junit

import cmu.pasta.fray.runtime.Delegate

class JunitRuntimeDelegate: Delegate() {
  override fun onThreadStart(t: Thread?) {
    Recorder.logThread()
  }
}
