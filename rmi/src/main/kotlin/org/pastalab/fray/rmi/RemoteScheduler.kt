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
) : Serializable {
  override fun toString(): String {
    return "Thread index: ${threadIndex}\n" +
        "Thread name: ${threadName}\n" +
        "Thread state: ${state}\n" +
        if (waitingFor != null) {
          "Thread is waiting for: ${waitingFor.resourceId}\n"
        } else {
          ""
        } +
        if (acquired.isNotEmpty()) {
          "Thread has acquired: ${acquired.joinToString(", ") { it.resourceId.toString() }}\n"
        } else {
          ""
        } +
        "Stack trace:\n" + stackTraces.joinToString("\n")
  }
}

interface RemoteScheduler : Remote {
  @Throws(RemoteException::class) fun scheduleNextOperation(threads: List<ThreadInfo>): Int

  companion object {
    const val NAME = "RemoteScheduler"
  }
}
