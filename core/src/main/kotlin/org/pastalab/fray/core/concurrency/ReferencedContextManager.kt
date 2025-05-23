package org.pastalab.fray.core.concurrency

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
    if (!hasContext(obj)) {
      objMap[id] = Pair(contextProducer(obj), IdentityPhantomReference(obj, queue))
      gc()
    }
    return objMap[id]!!.first
  }

  fun hasContext(obj: Any): Boolean {
    val id = System.identityHashCode(obj)
    return objMap.containsKey(id)
  }

  fun addContext(obj: Any, context: T) {
    val id = System.identityHashCode(obj)
    objMap[id] = Pair(context, IdentityPhantomReference(obj, queue))
    gc()
  }

  fun done(reset: Boolean = true) {
    gc()
    if (reset) {
      objMap.clear()
    }
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
