package org.pastalab.fray.core.concurrency.operations

class InterruptPendingOperation(val waitingObject: Any) : NonRacingOperation() {}
