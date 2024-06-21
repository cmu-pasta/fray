package cmu.pasta.fray.core.concurrency.operations

class LockBlocking(val lock: Any) : NonRacingOperation() {}
