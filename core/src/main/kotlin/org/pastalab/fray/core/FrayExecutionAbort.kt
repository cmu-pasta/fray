package org.pastalab.fray.core

// We want to use FrayExecutionAbortion to terminate the execution of Threads.
// We use Error here to avoid being caught by normal exception handling mechanisms.
class FrayExecutionAbort : Error("Fray execution aborted") {}
