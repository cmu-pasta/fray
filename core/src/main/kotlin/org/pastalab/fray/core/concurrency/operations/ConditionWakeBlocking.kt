package org.pastalab.fray.core.concurrency.operations

import java.util.concurrent.locks.Condition

class ConditionWakeBlocking(val condition: Condition) : NonRacingOperation() {}
