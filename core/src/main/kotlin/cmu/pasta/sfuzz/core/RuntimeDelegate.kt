package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.concurrency.SFuzzThread
import cmu.pasta.sfuzz.core.concurrency.Sync
import cmu.pasta.sfuzz.runtime.Delegate

class RuntimeDelegate: Delegate() {
    override fun onThreadStart(t: Thread) {
        if (t is SFuzzThread) return
        GlobalContext.registerThread(t)
    }

    override fun onThreadStartDone(t: Thread) {
        if (t is SFuzzThread) return
        GlobalContext.registerThreadDone(t)
    }

    override fun onThreadRun() {
        if (Thread.currentThread() is SFuzzThread) return
        GlobalContext.onThreadRun()
    }

    override fun onThreadEnd() {
        if (Thread.currentThread() is SFuzzThread) return
        GlobalContext.threadCompleted(Thread.currentThread())
    }

    override fun onObjectWait(o: Any) {
        if (o is SFuzzThread) return
        if (o is Sync) return  // Do not propagate if o is Sync
        GlobalContext.objectWait(o)
    }

    override fun onObjectWaitDone(o: Any) {
        if (o is SFuzzThread) return
        if (o is Sync) return
        GlobalContext.objectWaitDone(o)
    }

    override fun onObjectNotify(o: Any) {
        if (o is SFuzzThread) return
        if (o is Sync) return
        GlobalContext.objectNotify(o)
    }

    override fun onObjectNotifyAll(o: Any) {
        if (o is SFuzzThread) return
        if (o is Sync) return
        GlobalContext.objectNotifyAll(o)
    }

    override fun onObjectNotifyDone(o: Any) {
        if (o is SFuzzThread) return
        if (o is Sync) return
        GlobalContext.objectNotifyDone(o)
    }
}