package org.pastalab.fray.mcp

import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject
import java.util.concurrent.CountDownLatch
import org.pastalab.fray.rmi.Constant
import org.pastalab.fray.rmi.RemoteScheduler
import org.pastalab.fray.rmi.TestStatusObserver
import org.pastalab.fray.rmi.ThreadInfo

class StandaloneMCPScheduler : RemoteScheduler {
  val testStatusObserver =
      object : TestStatusObserver {
        override fun onExecutionStart() {
          server.onExecutionStart()
        }

        override fun onReportError(throwable: Throwable) {}

        override fun onExecutionDone(bugFound: Throwable?) {
          server.onExecutionDone(bugFound)
        }

        override fun saveToReportFolder(path: String) {}
      }
  var cdl = CountDownLatch(1)
  var selectedThreadIndex = 0
  val scheduleResultListener =
      object : ScheduleResultListener {
        override fun scheduled(thread: ThreadInfo) {
          selectedThreadIndex = thread.threadIndex
          cdl.countDown()
        }
      }

  val server = SchedulerServer(listOf(scheduleResultListener), false)

  override fun scheduleNextOperation(threads: List<ThreadInfo>, selectedThread: ThreadInfo?): Int {
    cdl = CountDownLatch(1)
    server.newSchedulingRequestReceived(threads, selectedThread)
    cdl.await()
    val selected = threads.firstOrNull { it.threadIndex == selectedThreadIndex }
    return if (selected != null) threads.indexOf(selected) else -1
  }
}

fun main() {
  val registry: Registry = LocateRegistry.createRegistry(Constant.REGISTRY_PORT)
  val scheduler = StandaloneMCPScheduler()
  val schedulerStub = UnicastRemoteObject.exportObject(scheduler, 15214) as RemoteScheduler
  registry.bind(RemoteScheduler.NAME, schedulerStub)
  val observerStub =
      UnicastRemoteObject.exportObject(scheduler.testStatusObserver, 15214) as TestStatusObserver
  registry.bind(TestStatusObserver.NAME, observerStub)
}
