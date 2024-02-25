package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.concurrency.Sync
import cmu.pasta.sfuzz.runtime.Delegate

class RuntimeDelegate: Delegate() {
    override fun onThreadStart(t: Thread) {
        GlobalContext.registerThread(t)
    }

    override fun onThreadStartDone(t: Thread) {
        GlobalContext.registerThreadDone(t)
    }

    override fun onThreadRun() {
        GlobalContext.onThreadRun()
    }

    override fun onThreadEnd() {
        GlobalContext.threadCompleted(Thread.currentThread())
    }

    override fun onObjectWait(o: Any) {
        if (o is Sync) return  // Do not propagate if o is Sync
        GlobalContext.objectWait(o)
    }

    override fun onObjectWaitDone(o: Any) {
        if (o is Sync) return
        GlobalContext.objectWaitDone(o)
    }

    override fun onObjectNotify(o: Any) {
        if (o is Sync) return
        GlobalContext.objectNotify(o)
    }

    override fun onObjectNotifyDone(o: Any) {
        if (o is Sync) return
        GlobalContext.objectNotifyDone(o)
    }
}