package org.pastalab.fray.core.concurrency.locks

import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue

class IdentityPhantomReference<T>(referent: T, queue: ReferenceQueue<in T>) :
    PhantomReference<T>(referent, queue) {
  val id = System.identityHashCode(referent)
}

class ReferencedContextManager<T>(val contextProducer: (Any) -> T) {
  val queue = ReferenceQueue<Any>()
  val objMap = mutableMapOf<Int, Pair<T, IdentityPhantomReference<*>>>()

  fun getContext(obj: Any): T {
    val id = System.identityHashCode(obj)
    if (!objMap.containsKey(id)) {
      objMap[id] = Pair(contextProducer(obj), IdentityPhantomReference(obj, queue))
      gc()
    }
    return objMap[id]!!.first
  }

  fun addContext(lock: Any, context: T) {
    val id = System.identityHashCode(lock)
    objMap[id] = Pair(context, IdentityPhantomReference(lock, queue))
    gc()
  }

  fun done() {
    gc()
    objMap.clear()
  }

  fun gc() {
    var ref = queue.poll()
    while (ref != null) {
      val id = (ref as IdentityPhantomReference<*>).id
      objMap.remove(id)
      ref = queue.poll()
    }
  }
}
