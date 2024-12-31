package org.pastalab.fray.idea.debugger

import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebuggerManagerListener

class FrayDebuggerManagerListener: XDebuggerManagerListener {
//  val scheduler = FrayDebuggerScheduler()
  init {
//    val stub = UnicastRemoteObject.exportObject(scheduler, 15214) as RemoteScheduler
//    val registry = LocateRegistry.createRegistry(1099)
//    registry.bind("RemoteScheduler", stub)
//    registry.unbind("RemoteScheduler")
  }
  override fun processStarted(debugProcess: XDebugProcess) {
    super.processStarted(debugProcess)
  }

  override fun processStopped(debugProcess: XDebugProcess) {
    super.processStopped(debugProcess)
  }
}
