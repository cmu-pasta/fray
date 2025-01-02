package org.pastalab.fray.idea.debugger

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.util.ui.UIUtil
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebugSessionListener
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject
import org.pastalab.fray.idea.ui.SchedulerPanel
import org.pastalab.fray.rmi.RemoteScheduler

class FrayDebuggerManager(val debugSession: XDebugSession) :
    XDebugSessionListener, ProcessListener {
  val schedulerPanel: SchedulerPanel = SchedulerPanel(debugSession.project)
  val scheduler = FrayDebuggerScheduler(schedulerPanel, debugSession)

  init {
    val stub = UnicastRemoteObject.exportObject(scheduler, 15214) as RemoteScheduler
    registry.bind("RemoteScheduler", stub)
  }

  override fun startNotified(event: ProcessEvent) {
    val container = SimpleToolWindowPanel(false, true)
    container.setContent(schedulerPanel)
    val content =
        debugSession.ui.createContent(
            SchedulerPanel.CONTENT_ID, container, "Fray Scheduler", null, null)
    content.isCloseable = false
    UIUtil.invokeLaterIfNeeded { debugSession.ui.addContent(content) }
  }

  override fun sessionPaused() {
    val sc = debugSession.suspendContext
  }

  fun stop() {
    registry.unbind("RemoteScheduler")
    UnicastRemoteObject.unexportObject(scheduler, true)
  }

  companion object {
    val registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT)
  }
}
