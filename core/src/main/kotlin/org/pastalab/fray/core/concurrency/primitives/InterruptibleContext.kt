package org.anonlab.fray.core.concurrency.primitives

import org.anonlab.fray.core.concurrency.operations.InterruptionType

interface InterruptibleContext {
  /** A primitive should implement this method if it supports interruption. */
  fun unblockThread(tid: Long, type: InterruptionType): Boolean
}
