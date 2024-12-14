package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.locks.Interruptible

class LockBlocking(val interruptible: Interruptible, timed: Boolean) :
    TimedBlockingOperation(timed) {}
