package cmu.pasta.fray.core.concurrency.locks

import cmu.pasta.fray.core.ThreadContext

class LockWaiter(val canInterrupt: Boolean, val thread: ThreadContext) {}
