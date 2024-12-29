package org.pastalab.fray.idea

import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebugSessionListener

class FrayDebuggerManager(val debugSession: XDebugSession): XDebugSessionListener {
  override fun sessionPaused() {
    val sc = debugSession.suspendContext
  }
}
