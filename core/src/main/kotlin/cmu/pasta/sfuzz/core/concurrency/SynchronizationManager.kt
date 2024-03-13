package cmu.pasta.sfuzz.core.concurrency

class SynchronizationManager {
    val synchronizationPoints = mutableMapOf<Int, Sync>()

    fun createWait(obj: Any, times: Int) {
        val id = System.identityHashCode(obj)
        assert(!synchronizationPoints.contains(id))
        synchronizationPoints[id] = Sync(times)
    }

    fun wait(obj: Any) {
        val id = System.identityHashCode(obj)
        synchronizationPoints[id]?.block()
        synchronizationPoints.remove(id)
    }

    fun signal(obj: Any) {
        val id = System.identityHashCode(obj)
        synchronizationPoints[id]?.unblock()
    }

}