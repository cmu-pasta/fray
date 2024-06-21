package cmu.pasta.fray.core.concurrency.operations

class InterruptPendingOperation(val waitingObject: Any) : NonRacingOperation() {}
