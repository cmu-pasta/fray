package cmu.pasta.sfuzz.core.concurrency.locks

import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue


class IdentityPhantomReference<T>(referent: T, queue: ReferenceQueue<in T>) : PhantomReference<T>(referent, queue) {
    val id = System.identityHashCode(referent)
}
class ReferencedContextManager<T>(val contextProducer: (Any) -> T) {
    val queue = ReferenceQueue<Any>()
    val lockMap = mutableMapOf<Int, T>()
    fun getLockContext(lock: Any): T {
        val id = System.identityHashCode(lock)
        if (!lockMap.containsKey(id)) {
            lockMap[id] = contextProducer(lock)
            IdentityPhantomReference(lock, queue)
            gc()
        }
        return lockMap[id]!!
    }

    fun addContext(lock: Any, context: T) {
        val id = System.identityHashCode(lock)
        lockMap[id] = context
        IdentityPhantomReference(lock, queue)
        gc()
    }

    fun done() {
        gc()
    }

    fun gc() {
        var ref = queue.poll()
        while (ref != null) {
            val id = (ref as IdentityPhantomReference<*>).id
            lockMap.remove(id)
            ref = queue.poll()
        }
    }
}