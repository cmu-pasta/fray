package org.pastalab.fray.idea.debugger

import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebuggerManagerListener
import org.pastalab.fray.idea.FrayConstants.FRAY_DEBUGGER_KEY
import org.pastalab.fray.idea.execute.FrayGradleRunConfiguration
import org.pastalab.fray.rmi.Constant.FRAY_DEBUGGER_DEBUG
import org.pastalab.fray.rmi.Constant.FRAY_DEBUGGER_DISABLED
import org.pastalab.fray.rmi.Constant.FRAY_DEBUGGER_REPLAY

class FrayDebuggerManagerListener : XDebuggerManagerListener {
  var debuggerManager: FrayDebuggerManager? = null

  override fun processStarted(debugProcess: XDebugProcess) {
    val runProfile = debugProcess.session.runProfile
    val debuggerMode =
        if (runProfile is FrayGradleRunConfiguration) {
          runProfile.getUserData<String>(FRAY_DEBUGGER_KEY)
        } else {
          FRAY_DEBUGGER_DISABLED
        }
    if (debuggerMode == FRAY_DEBUGGER_DEBUG) {
      debuggerManager = FrayDebuggerManager(debugProcess.session, false)
    } else if (debuggerMode == FRAY_DEBUGGER_REPLAY) {
      debuggerManager = FrayDebuggerManager(debugProcess.session, true)
    }
    debuggerManager?.let {
      debugProcess.session.addSessionListener(it)
      debugProcess.processHandler.addProcessListener(it)
    }
    super.processStarted(debugProcess)
  }

  override fun processStopped(debugProcess: XDebugProcess) {
    super.processStopped(debugProcess)
    debuggerManager?.stop()
  }
}
