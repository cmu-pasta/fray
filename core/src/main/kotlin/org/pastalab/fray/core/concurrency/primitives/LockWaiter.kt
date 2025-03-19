package org.anonlab.fray.core.concurrency.primitives

import org.anonlab.fray.core.ThreadContext

class LockWaiter(val canInterrupt: Boolean, val thread: ThreadContext) {}
