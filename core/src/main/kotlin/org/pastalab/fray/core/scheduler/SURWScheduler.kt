package org.pastalab.fray.core.scheduler

import kotlin.math.max
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.MemoryOperation
import org.pastalab.fray.core.concurrency.operations.RacingOperation
import org.pastalab.fray.core.concurrency.operations.ThreadStartOperation
import org.pastalab.fray.core.randomness.ControlledRandom
import org.pastalab.fray.core.utils.Utils.verifyOrReport

data class MemoryAccessRecord(
    val threadId: Int,
    val locationHashes: MutableSet<Int>,
    var isInteresting: Boolean = false
) {}


// See https://dl.acm.org/doi/10.1145/3669940.3707214
@Serializable
class SURWScheduler(
    val rand: ControlledRandom,
    val executionLengths: Map<Int, Int>,
    val interestingOperations: Set<Int>
) : Scheduler {
  // A mapping between thread id and its weight
  // We should use `ThreadContext.id` instead of `Thread.id` because
  // this information is shared across executions.
  @Transient val weight = HashMap(executionLengths)
  @Transient val blocked = mutableSetOf<Int>()
  @Transient var nextIntendedThread = -1

  @Transient val createdThreads = mutableSetOf<Int>()
  @Transient val childThreads = mutableMapOf<Int, MutableSet<Int>>()

  // These three fields are only used for the first trial to
  // construct interesting operations map.
  @Transient val interestingObjectMap = mutableMapOf<Int, MemoryAccessRecord>()
  @Transient val interestingOperationCache = mutableSetOf<Int>()
  @Transient val threadExecutionLengthCache = mutableMapOf<Int, Int>()

  fun updateNextIntendedThread(threads: List<ThreadContext>) {
    val totalWeight = threads.sumOf { weight[it.index] ?: 0 }
    val selectedThreadWeight = (rand.nextInt() % totalWeight) + 1
    var currentWeight = 0

    for (thread in threads) {
      val threadWeight = weight[thread.index] ?: 0
      currentWeight += threadWeight
      if (currentWeight >= selectedThreadWeight && (threadWeight != 0)) {
        nextIntendedThread = thread.index
        break
      }
    }
  }

  constructor() : this(ControlledRandom(), mutableMapOf(), mutableSetOf())

  fun checkNewThreads(threads: List<ThreadContext>) {
    threads
        .filter { !createdThreads.contains(it.index) }
        .forEach {
          createdThreads.add(it.index)
          val operation = it.pendingOperation
          verifyOrReport(operation is ThreadStartOperation)
          val parentId = (operation as ThreadStartOperation).parentId
          if (parentId == -1) return@forEach
          childThreads.getOrPut(parentId) { mutableSetOf() }.add(it.index)
          val childWeight = weight.getOrDefault(it.index, 0)
          val parentWeight = weight.getOrDefault(parentId, 0)
          if (nextIntendedThread == parentId) {
            val randomWeight = rand.nextInt() % max(parentWeight, 1) + 1
            if (childWeight > randomWeight) {
              nextIntendedThread = it.index
            }
          }
          weight[parentId] = max(parentWeight - childWeight, 0)
        }
  }

  override fun scheduleNextOperation(
      threads: List<ThreadContext>,
      allThreads: List<ThreadContext>
  ): ThreadContext {
    // First we need to update weights if new threads are created.
    checkNewThreads(threads)

    val filteredThreads = threads.filter { !blocked.contains(it.index) }.toList()

    if (filteredThreads.isEmpty()) {
      nextIntendedThread = -1
      blocked.clear()
      return scheduleNextOperation(threads, allThreads)
    }

    val nextThreadIndex = rand.nextInt() % filteredThreads.size
    val nextThread = filteredThreads[nextThreadIndex]
    val nextThreadOperation = nextThread.pendingOperation

    if (nextThreadOperation is RacingOperation &&
        interestingOperations.contains(nextThreadOperation.stackTraceHash)) {
      // See:
      // https://github.com/zhaohuanqdcn/SURW/blob/24e8ed919ce34ebfcd4b2a6a997d78b468eec91f/src/main.zig#L700
      if ((weight[nextThread.index] ?: 0) == 0) {
        weight[nextThread.index] = 1
      }
      if (nextIntendedThread == -1) {
        updateNextIntendedThread(filteredThreads)
      }
      if (nextThread.index == nextIntendedThread) {
        weight[nextThread.index] = weight[nextThread.index]!! - 1
        blocked.clear()
        nextIntendedThread = -1
      } else {
        blocked.add(nextThread.index)
        return scheduleNextOperation(threads, allThreads)
      }
      if (executionLengths.isEmpty()) {
        threadExecutionLengthCache[nextThread.index] =
            (threadExecutionLengthCache[nextThread.index] ?: 0) + 1
      }
    }

    // We may want to build the interesting operations if it is empty.
    if (interestingOperations.isEmpty()) {
      constructInterestingOperation(nextThread)
    }

    return nextThread
  }

  private fun constructInterestingOperation(thread: ThreadContext) {
    val operation = thread.pendingOperation
    if (operation is MemoryOperation) {
      if (interestingObjectMap.containsKey(operation.resource)) {
        val objectRecord = interestingObjectMap[operation.resource]!!
        if (objectRecord.isInteresting) {
          interestingOperationCache.add(operation.stackTraceHash)
        } else if (objectRecord.threadId == thread.index) {
          objectRecord.locationHashes.add(operation.stackTraceHash)
        } else {
          objectRecord.isInteresting = true
          interestingOperationCache.add(operation.stackTraceHash)
          interestingOperationCache.addAll(objectRecord.locationHashes)
        }
      } else {
        interestingObjectMap[operation.resource] =
            MemoryAccessRecord(thread.index, mutableSetOf(operation.stackTraceHash), false)
      }
    }
  }

  private fun buildThreadWeights(): MutableMap<Int, Int> {
    val threadWeights = mutableMapOf<Int, Int>()
    if (threadExecutionLengthCache.isNotEmpty()) {
      for (thread in createdThreads) {
        if (threadWeights.contains(thread)) continue
        buildThreadWeightsRecursive(thread, threadWeights)
      }
    }
    return threadWeights
  }

  private fun buildThreadWeightsRecursive(threadId: Int, threadWeights: MutableMap<Int, Int>): Int {
    if (!threadWeights.contains(threadId)) {
      val weight =
          (threadExecutionLengthCache[threadId] ?: 0) +
              childThreads.getOrDefault(threadId, mutableSetOf()).sumOf {
                buildThreadWeightsRecursive(it, threadWeights)
              }
      threadWeights[threadId] = weight
    }
    return threadWeights[threadId]!!
  }

  override fun nextIteration(): Scheduler {
    return SURWScheduler(
        ControlledRandom(),
        if (executionLengths.isEmpty()) {
          buildThreadWeights()
        } else {
          executionLengths
        },
        if (interestingOperations.isEmpty()) {
          interestingOperationCache
        } else {
          interestingOperations
        })
  }
}
