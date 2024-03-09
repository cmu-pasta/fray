package cmu.pasta.sfuzz.core.concurrency.operations

import cmu.pasta.sfuzz.runtime.MemoryOpType

class ObjectWaitOperation(obj: Int): ConflictingOperation(obj, MemoryOpType.MEMORY_WRITE) {
}