package org.pastalab.fray.core.concurrency.context

import org.pastalab.fray.core.concurrency.operations.InterruptionType

interface InterruptibleContext {
  /** A primitive should implement this method if it supports interruption. */
  fun unblockThread(tid: Long, type: InterruptionType): Boolean
}
