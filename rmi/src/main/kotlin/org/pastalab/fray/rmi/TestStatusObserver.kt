package org.pastalab.fray.rmi

import java.nio.file.Path
import java.rmi.Remote
import java.rmi.RemoteException

interface TestStatusObserver : Remote {
  @Throws(RemoteException::class) fun onExecutionStart()

  @Throws(RemoteException::class) fun onReportError(throwable: Throwable)

  @Throws(RemoteException::class) fun onExecutionDone(bugFound: Throwable?)

  @Throws(RemoteException::class) fun saveToReportFolder(path: Path)

  companion object {
    const val NAME = "TestStatusObserver"
  }
}
