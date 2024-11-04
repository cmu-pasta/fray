package org.pastalab.fray.core.concurrency.operations

import java.util.concurrent.locks.Condition

class ConditionAwaitBlocked(val condition: Condition, val canInterrupt: Boolean, timed: Boolean) :
    TimedBlockingOperation(timed) {}
