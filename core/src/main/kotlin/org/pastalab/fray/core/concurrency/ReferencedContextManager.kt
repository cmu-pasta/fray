package org.pastalab.fray.core.concurrency

import org.pastalab.fray.core.map.WeakIdentityHashMap

class ReferencedContextManager<T>(val contextProducer: (Any) -> T) {
  val objMap = WeakIdentityHashMap<Any, T>()

  fun getContext(obj: Any): T {
    if (!hasContext(obj)) {
      objMap[obj] = contextProducer(obj)
    }
    return objMap[obj]!!
  }

  fun hasContext(obj: Any): Boolean {
    return objMap.containsKey(obj)
  }

  fun addContext(obj: Any, context: T) {
    objMap[obj] = context
  }

  fun done(reset: Boolean = true) {
    if (reset) {
      objMap.clear()
    }
  }
}
