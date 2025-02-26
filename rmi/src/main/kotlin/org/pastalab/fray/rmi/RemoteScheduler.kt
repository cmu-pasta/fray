package org.pastalab.fray.rmi

import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException
import kotlin.jvm.Throws

enum class ThreadState {
  Runnable,
  Running,
  Blocked,
  Completed,
  Created,
}

data class ThreadInfo(
    val threadName: String,
    val index: Long,
    val state: ThreadState,
    val stackTraces: List<StackTraceElement>,
) : Serializable

interface RemoteScheduler : Remote {
  @Throws(RemoteException::class) fun scheduleNextOperation(threads: List<ThreadInfo>): Int
}
