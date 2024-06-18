package cmu.pasta.fray.core.concurrency.locks

data class ThreadWaitsForInfo(val id: Int, val canInterrupt: Boolean)
