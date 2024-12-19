package org.pastalab.fray.core.concurrency.primitives

data class ThreadWaitsForInfo(val id: Int, val canInterrupt: Boolean)
