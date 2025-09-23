package org.pastalab.fray.rmi

import java.rmi.Remote
import java.rmi.RemoteException

interface TestStatusObserver : Remote {
  @Throws(RemoteException::class) fun onExecutionStart()

  @Throws(RemoteException::class) fun onReportError(throwable: Throwable)

  @Throws(RemoteException::class) fun onExecutionDone(bugFound: Throwable?)

  @Throws(RemoteException::class) fun saveToReportFolder(path: String)

  companion object {
    const val NAME = "TestStatusObserver"
  }
}
