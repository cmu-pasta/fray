package org.pastalab.fray.idea.debugger

import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebuggerManagerListener
import org.pastalab.fray.rmi.RemoteScheduler
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject

class FrayDebuggerManagerListener: XDebuggerManagerListener {
  var debuggerManager: FrayDebuggerManager? = null

  override fun processStarted(debugProcess: XDebugProcess) {
    debuggerManager = FrayDebuggerManager(debugProcess.session)
    debugProcess.session.addSessionListener(debuggerManager!!)
    debugProcess.processHandler.addProcessListener(debuggerManager!!)
    super.processStarted(debugProcess)
  }

  override fun processStopped(debugProcess: XDebugProcess) {
    super.processStopped(debugProcess)
    debuggerManager?.stop()
  }
}
