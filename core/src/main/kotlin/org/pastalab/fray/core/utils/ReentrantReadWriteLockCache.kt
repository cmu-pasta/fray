package org.pastalab.fray.core.utils

import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import org.pastalab.fray.core.concurrency.ReferencedContextManager

/**
 * We need a static object to store the [java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock]
 * and [java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock] because if the lock is created
 * statically, we may lose track of the lock owner across test runs.
 */
object ReentrantReadWriteLockCache {
  val lockCache =
      ReferencedContextManager<WeakReference<ReentrantReadWriteLock>>({
        throw RuntimeException("Should not be called")
      })

  fun getLock(obj: Any): ReentrantReadWriteLock? {
    if (lockCache.hasContext(obj)) {
      return lockCache.getContext(obj).get()
    }
    return null
  }

  fun registerLock(lock: ReentrantReadWriteLock) {
    lockCache.addContext(lock.readLock(), WeakReference(lock))
    lockCache.addContext(lock.writeLock(), WeakReference(lock))
  }
}
