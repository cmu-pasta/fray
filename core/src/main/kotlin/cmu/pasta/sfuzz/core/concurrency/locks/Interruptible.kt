package cmu.pasta.sfuzz.core.concurrency.locks

interface Interruptible {
  fun interrupt(tid: Long)
}
