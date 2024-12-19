package org.pastalab.fray.core.concurrency.primitives

import org.pastalab.fray.core.ThreadContext

class LockWaiter(val canInterrupt: Boolean, val thread: ThreadContext) {}
