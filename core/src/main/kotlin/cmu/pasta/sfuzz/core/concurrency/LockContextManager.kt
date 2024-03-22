package cmu.pasta.sfuzz.core.concurrency

import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.*


class IdentityPhantomReference<T>(referent: T, queue: ReferenceQueue<in T>) : PhantomReference<T>(referent, queue) {
    val id = System.identityHashCode(referent)
}
class LockContextManager {
//    val queue = ReferenceQueue<Any>()
    val lockMap = mutableMapOf<Int, ReentrantLockContext>()
    fun getLockContext(lock: Any): ReentrantLockContext {

//        val emptyLocks = mutableListOf<Int>()
//        for (pair in lockMap.entries) {
//            if (pair.value.isEmpty()) {
//                emptyLocks.add(pair.key)
//            }
//        }
//        for (key in emptyLocks) {
//            lockMap.remove(key)
//        }
        println(lockMap.size)
        val id = System.identityHashCode(lock)
        if (!lockMap.containsKey(id)) {
            lockMap[id] = ReentrantLockContext()
        }
//        gc()
        return lockMap[id]!!
    }

//    fun done() {
//        lockMap.clear()
//    }
//
//    fun gc() {
//        var ref = queue.poll()
//        while (ref != null) {
//            val id = (ref as IdentityPhantomReference<*>).id
//            lockMap.remove(id)
//            ref = queue.poll()
//            println(lockMap.size)
//        }
//    }
}