package org.anonlab.fray.core.concurrency.operations

class InterruptPendingOperation(val waitingObject: Any) : NonRacingOperation() {}
