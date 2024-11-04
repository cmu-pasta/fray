package org.pastalab.fray.core.concurrency.operations

class ObjectWakeBlocked(val o: Any, val noTimeout: Boolean) : NonRacingOperation() {}
