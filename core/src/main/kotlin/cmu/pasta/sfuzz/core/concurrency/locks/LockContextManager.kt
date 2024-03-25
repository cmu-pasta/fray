package cmu.pasta.sfuzz.core.concurrency.locks

import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue


class IdentityPhantomReference<T>(referent: T, queue: ReferenceQueue<in T>) : PhantomReference<T>(referent, queue) {
    val id = System.identityHashCode(referent)
}
class LockContextManager {
    val queue = ReferenceQueue<Any>()
    val lockMap = mutableMapOf<Int, LockContext>()
    fun getLockContext(lock: Any): LockContext {
        val id = System.identityHashCode(lock)
        if (!lockMap.containsKey(id)) {
            lockMap[id] = ReentrantLockContext()
            IdentityPhantomReference(lock, queue)
            gc()
        }
        return lockMap[id]!!
    }

    fun addLockContext(lock: Any, context: LockContext) {
        val id = System.identityHashCode(lock)
        lockMap[id] = context
        IdentityPhantomReference(lock, queue)
        gc()
    }

    fun done() {
        lockMap.clear()
    }

    fun gc() {
        var ref = queue.poll()
        while (ref != null) {
            val id = (ref as IdentityPhantomReference<*>).id
            assert(lockMap[id]!!.isEmpty())
            lockMap.remove(id)
            ref = queue.poll()
        }
    }
}