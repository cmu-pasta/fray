package org.pastalab.fray.core.concurrency.primitives

interface InterruptibleContext {
  /** A primitive should implement this method if it supports interruption. */
  fun unblockThread(tid: Long, type: InterruptionType): Boolean
}
