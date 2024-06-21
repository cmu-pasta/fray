package cmu.pasta.fray.core.concurrency.operations

import java.util.concurrent.locks.Condition

class ConditionAwaitBlocking(val condition: Condition, val canInterrupt: Boolean) :
    NonRacingOperation() {}
