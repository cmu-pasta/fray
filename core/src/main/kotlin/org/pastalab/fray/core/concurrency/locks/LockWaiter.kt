package org.pastalab.fray.core.concurrency.locks

import org.pastalab.fray.core.ThreadContext

class LockWaiter(val canInterrupt: Boolean, val thread: ThreadContext) {}
