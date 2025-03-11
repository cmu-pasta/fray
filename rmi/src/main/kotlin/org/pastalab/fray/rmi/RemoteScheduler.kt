package org.pastalab.fray.rmi

import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException
import kotlin.jvm.Throws

enum class ThreadState {
  Runnable,
  MainExiting,
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

data class ResourceInfo(val resourceId: Int, val resourceType: ResourceType) : Serializable {
  override fun toString(): String {
    return "Id: $resourceId, Type: $resourceType"
  }
}

data class ThreadInfo(
    val threadName: String,
    val threadIndex: Int,
    val state: ThreadState,
    val stackTraces: List<StackTraceElement>,
    val waitingFor: ResourceInfo?,
    val acquired: Set<ResourceInfo>
) : Serializable

interface RemoteScheduler : Remote {
  @Throws(RemoteException::class) fun scheduleNextOperation(threads: List<ThreadInfo>): Int

  companion object {
    const val NAME = "RemoteScheduler"
  }
}
