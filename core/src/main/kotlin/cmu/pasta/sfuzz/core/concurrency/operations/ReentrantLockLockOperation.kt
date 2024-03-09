package cmu.pasta.sfuzz.core.concurrency.operations

import cmu.pasta.sfuzz.runtime.MemoryOpType

class ReentrantLockLockOperation(obj: Int): RacingOperation(obj, MemoryOpType.MEMORY_WRITE) {
    override fun isRacing(op: Operation): Boolean {
        if (op is ReentrantLockLockOperation){
            return op.resource == resource
        }
        return false
    }
}