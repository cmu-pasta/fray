package org.pastalab.fray.rmi

import java.rmi.Remote
import java.rmi.RemoteException
import kotlin.jvm.Throws

interface RemoteScheduler: Remote {
  @Throws(RemoteException::class)
  fun scheduleNextOperation(threads: List<Long>): Int
}
