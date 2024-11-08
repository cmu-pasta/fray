package org.pastalab.fray.core.concurrency.operations

class LockBlocking(val lock: Any, timed: Boolean) : TimedBlockingOperation(timed) {}
