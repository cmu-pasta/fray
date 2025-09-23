package org.pastalab.fray.idea.debugger

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebugSessionListener
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject
import org.pastalab.fray.idea.ui.FrayDebugPanel
import org.pastalab.fray.rmi.Constant
import org.pastalab.fray.rmi.RemoteScheduler
import org.pastalab.fray.rmi.TestStatusObserver

class FrayDebuggerManager(val debugSession: XDebugSession, val replayMode: Boolean) :
    XDebugSessionListener, ProcessListener {
  val testStatusObserver = FrayRemoteTestObserver(debugSession.project)
  val schedulerPanel: FrayDebugPanel = FrayDebugPanel(debugSession, replayMode)
  val scheduler = FrayDebuggerScheduler(schedulerPanel, debugSession, replayMode)

  init {
    if (replayMode) {
      testStatusObserver.observers.add(schedulerPanel)
    }
    val schedulerStub = UnicastRemoteObject.exportObject(scheduler, 15214) as RemoteScheduler
    registry.bind(RemoteScheduler.NAME, schedulerStub)
    val observerStub =
        UnicastRemoteObject.exportObject(testStatusObserver, 15214) as TestStatusObserver
    registry.bind(TestStatusObserver.NAME, observerStub)
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
    if (replayMode) {
      testStatusObserver.observers.remove(schedulerPanel)
    }
    schedulerPanel.stop()
    registry.unbind(RemoteScheduler.NAME)
    registry.unbind(TestStatusObserver.NAME)
    UnicastRemoteObject.unexportObject(scheduler, true)
    UnicastRemoteObject.unexportObject(testStatusObserver, true)
  }

  companion object {
    val registry: Registry = LocateRegistry.createRegistry(Constant.REGISTRY_PORT)
  }
}
