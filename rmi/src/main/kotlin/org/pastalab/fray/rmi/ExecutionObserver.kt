package org.pastalab.fray.rmi

import java.rmi.Remote
import java.rmi.RemoteException

interface ExecutionObserver : Remote {
  @Throws(RemoteException::class) fun onNewThread(newThread: Thread, parentThread: Thread)
}
