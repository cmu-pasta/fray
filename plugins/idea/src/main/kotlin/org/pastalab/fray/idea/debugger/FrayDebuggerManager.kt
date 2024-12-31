package org.pastalab.fray.idea.debugger

import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebugSessionListener

class FrayDebuggerManager(val debugSession: XDebugSession): XDebugSessionListener {
  override fun sessionPaused() {
    val sc = debugSession.suspendContext
  }
}
