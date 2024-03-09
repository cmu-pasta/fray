package cmu.pasta.sfuzz.core.concurrency.operations

import cmu.pasta.sfuzz.runtime.MemoryOpType
import kotlinx.serialization.Serializable

class ReentrantLockLockOperation(obj: Int): ConflictingOperation(obj, MemoryOpType.MEMORY_WRITE) {
}