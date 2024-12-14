package org.pastalab.fray.core.concurrency.locks

interface Interruptible {
  fun interrupt(tid: Long, noTimeout: Boolean)
}
