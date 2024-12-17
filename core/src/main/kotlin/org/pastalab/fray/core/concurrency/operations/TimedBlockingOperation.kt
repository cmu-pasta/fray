package org.pastalab.fray.core.concurrency.operations

import org.pastalab.fray.core.concurrency.primitives.Interruptible

abstract class TimedBlockingOperation(val timed: Boolean) : NonRacingOperation(), Interruptible {}
