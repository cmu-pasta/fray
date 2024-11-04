package org.pastalab.fray.core.concurrency.operations

open class TimedBlockingOperation(val timed: Boolean) : NonRacingOperation() {}
