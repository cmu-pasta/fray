package cmu.pasta.fray.core.concurrency.locks

interface Interruptible {
  fun interrupt(tid: Long)
}
