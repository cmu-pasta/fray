package org.pastalab.fray.idea.debugger

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSessionListener
import com.intellij.xdebugger.impl.XDebugSessionImpl
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject
import org.pastalab.fray.idea.ui.FrayDebugPanel
import org.pastalab.fray.rmi.Constant.REGISTRY_PORT
import org.pastalab.fray.rmi.RemoteScheduler
import org.pastalab.fray.rmi.TestStatusObserver

class FrayDebuggerManager(val debugProcess: XDebugProcess, val replayMode: Boolean) :
    XDebugSessionListener, ProcessListener {
  val testStatusObserver = FrayRemoteTestObserver(debugProcess.session.project)
  val schedulerPanel: FrayDebugPanel = FrayDebugPanel(debugProcess.session, replayMode)
  val scheduler = FrayDebuggerScheduler(schedulerPanel, debugProcess.session, replayMode)

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
    val session = debugProcess.session
    if (session is XDebugSessionImpl) {
      session.runWhenUiReady {
        val container = SimpleToolWindowPanel(false, true)
        container.setContent(schedulerPanel)
        val content =
            it.createContent(
                FrayDebugPanel.CONTENT_ID,
                container,
                "Fray Scheduler",
                null,
                null,
            )
        content.isCloseable = false
        it.addContent(content)
      }
    }
  }

  companion object {
    val registry: Registry = LocateRegistry.createRegistry(REGISTRY_PORT)
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
}
