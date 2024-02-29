package cmu.pasta.sfuzz.core

import cmu.pasta.sfuzz.core.concurrency.SFuzzThread
import cmu.pasta.sfuzz.core.concurrency.Sync
import cmu.pasta.sfuzz.runtime.Delegate

class RuntimeDelegate: Delegate() {

    var entered = ThreadLocal.withInitial { false }

    private fun checkEntered(): Boolean {
        if (entered.get()) {
            return true
        }
        entered.set(true)
        if (Thread.currentThread() is SFuzzThread) {
            entered.set(false)
            return true
        }
        // We do not process threads created outside of application.
        if (!GlobalContext.registeredThreads.containsKey(Thread.currentThread().threadId())) {
            entered.set(false)
            return true
        }
        return false
    }

    override fun onThreadStart(t: Thread) {
        if (checkEntered()) return
        if (t is SFuzzThread) return
        GlobalContext.threadStart(t)
        entered.set(false)
    }

    override fun onThreadStartDone(t: Thread) {
        if (checkEntered()) return
        if (t is SFuzzThread) return
        GlobalContext.threadStartDone(t)
        entered.set(false)
    }

    override fun onThreadRun() {
        if (checkEntered()) return
        if (Thread.currentThread() is SFuzzThread) return
        GlobalContext.threadRun()
        entered.set(false)
    }

    override fun onThreadEnd() {
        if (checkEntered()) return
        if (Thread.currentThread() is SFuzzThread) return
        GlobalContext.threadCompleted(Thread.currentThread())
        entered.set(false)
    }

    override fun onObjectWait(o: Any) {
        if (checkEntered()) return
        if (o is SFuzzThread) return
        if (o is Sync) return  // Do not propagate if o is Sync
        GlobalContext.objectWait(o)
        entered.set(false)
    }

    override fun onObjectNotify(o: Any) {
        if (checkEntered()) return
        if (o is SFuzzThread) return
        if (o is Sync) return
        GlobalContext.objectNotify(o)
        entered.set(false)
    }

    override fun onObjectNotifyAll(o: Any) {
        if (checkEntered()) return
        if (o is SFuzzThread) return
        if (o is Sync) return
        GlobalContext.objectNotifyAll(o)
        entered.set(false)
    }

    override fun onReentrantLockLock(l: Any) {
        if (checkEntered()) return
        GlobalContext.reentrantLockLock(l)
        entered.set(false)
    }

    override fun onReentrantLockTryLock(l: Any) {
        if (checkEntered()) return
        GlobalContext.reentrantLockTrylock(l)
        entered.set(false)
    }

    override fun onReentrantLockUnlock(l: Any) {
        if (checkEntered()) return
        GlobalContext.reentrantLockUnlock(l)
        entered.set(false)
    }

    override fun onAtomicOperation(o: Any) {
        if (checkEntered()) return
        GlobalContext.atomicOperation(o)
        entered.set(false)
    }

    override fun onFieldRead(o: Any?, owner: String?, name: String?, descriptor: String?) {
        if (checkEntered()) return
        entered.set(false)
    }

    override fun onFieldWrite(o: Any?, owner: String?, name: String?, descriptor: String?) {
        if (checkEntered()) return
        entered.set(false)
    }


    override fun onStaticFieldRead(owner: String?, name: String?, descriptor: String?) {
        if (checkEntered()) return
        entered.set(false)
    }

    override fun onStaticFieldWrite(owner: String?, name: String?, descriptor: String?) {
        if (checkEntered()) return
        entered.set(false)
    }
}