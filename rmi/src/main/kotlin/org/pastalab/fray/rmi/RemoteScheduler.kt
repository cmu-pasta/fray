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

enum class ResourceType {
  LOCK,
  CONDITION,
  SEMAPHORE,
  CDL,
  PARK,
  SYNCURITY_CONDITION,
}

data class ResourceInfo(val resourceId: Int, val resourceType: ResourceType)

data class ThreadInfo(
    val threadName: String,
    val index: Long,
    val state: ThreadState,
    val stackTraces: List<StackTraceElement>,
    val waitingOn: ResourceInfo?,
    val acquired: Set<ResourceInfo>
) : Serializable

interface RemoteScheduler : Remote {
  @Throws(RemoteException::class) fun scheduleNextOperation(threads: List<ThreadInfo>): Int
}
