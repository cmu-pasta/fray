package org.pastalab.fray.core.concurrency.operations

import java.util.concurrent.locks.Condition

class ConditionWakeBlocked(val condition: Condition, val noTimeout: Boolean) :
    NonRacingOperation() {}
