package cmu.pasta.sfuzz.core.concurrency.operations

import cmu.pasta.sfuzz.runtime.MemoryOpType

abstract class ConflictingOperation(val resource: Int, val type: MemoryOpType): Operation {
}