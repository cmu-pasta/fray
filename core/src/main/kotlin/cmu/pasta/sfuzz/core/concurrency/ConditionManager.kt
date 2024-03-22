package cmu.pasta.sfuzz.core.concurrency

class ConditionManager {
    val waitingThreads: MutableMap<Int, MutableList<Long>> = mutableMapOf()

    fun addWaitingThread(waitingObject: Any, t: Thread) {
        val id = System.identityHashCode(waitingObject)
        if (id !in waitingThreads) {
            waitingThreads[id] = mutableListOf()
        }
        waitingThreads[id]!!.add(t.id)
    }
}