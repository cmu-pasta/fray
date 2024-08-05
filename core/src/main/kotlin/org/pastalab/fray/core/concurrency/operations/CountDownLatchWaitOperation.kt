package org.pastalab.fray.core.concurrency.operations

import java.util.concurrent.CountDownLatch

class CountDownLatchAwaitBlocking(val latch: CountDownLatch) : NonRacingOperation() {}
