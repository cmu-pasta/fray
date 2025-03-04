package org.pastalab.fray.idea.debugger

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebugSessionListener
import java.rmi.Remote
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject
import org.pastalab.fray.idea.ui.FrayDebugPanel
import org.pastalab.fray.rmi.RemoteScheduler
import org.pastalab.fray.rmi.ScheduleObserver

class FrayDebuggerManager(val debugSession: XDebugSession) :
    XDebugSessionListener, ProcessListener {
  val scheduleObserver = FrayScheduleObserver(debugSession.project)
  val schedulerPanel: FrayDebugPanel = FrayDebugPanel(debugSession.project, scheduleObserver)
  val scheduler = FrayDebuggerScheduler(schedulerPanel, debugSession)

  init {
    val schedulerStub = UnicastRemoteObject.exportObject(scheduler, 15214) as RemoteScheduler
    registry.bind(RemoteScheduler.NAME, schedulerStub)
    val observerStub = UnicastRemoteObject.exportObject(scheduleObserver, 15214) as Remote
    registry.bind(ScheduleObserver.NAME, observerStub)
  }

  override fun startNotified(event: ProcessEvent) {
    val container = SimpleToolWindowPanel(false, true)
    container.setContent(schedulerPanel)
    val content =
        debugSession.ui.createContent(
            FrayDebugPanel.CONTENT_ID, container, "Fray Scheduler", null, null)
    content.isCloseable = false

    ApplicationManager.getApplication().invokeLater { debugSession.ui.addContent(content) }
  }

  override fun sessionPaused() {}

  fun stop() {
    schedulerPanel.stop()
    registry.unbind(RemoteScheduler.NAME)
    registry.unbind(ScheduleObserver.NAME)
    UnicastRemoteObject.unexportObject(scheduler, true)
    UnicastRemoteObject.unexportObject(scheduleObserver, true)
  }

  companion object {
    val registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT)
  }
}
