package org.pastalab.fray.core.concurrency.operations

class ObjectWaitBlock(val o: Any, timed: Boolean) : TimedBlockingOperation(timed) {}
