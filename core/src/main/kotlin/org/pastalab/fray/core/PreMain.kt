package org.pastalab.fray.core

import java.lang.instrument.Instrumentation
import org.pastalab.fray.core.command.MainCommand
import org.pastalab.fray.core.delegates.DelegateSynchronizer
import org.pastalab.fray.core.delegates.RuntimeDelegate
import org.pastalab.fray.instrumentation.base.ApplicationCodeTransformer
import org.pastalab.fray.runtime.Delegate
import org.pastalab.fray.runtime.Runtime

fun premain(arguments: String, instrumentation: Instrumentation) {
  val args = arguments.split(":") + "--run-config" + "empty"
  val applicationCodeTransformer = ApplicationCodeTransformer()
  instrumentation.addTransformer(applicationCodeTransformer)
  val config = MainCommand().apply { main(args) }.toConfiguration()
  if (config.noFray) {
    instrumentation.removeTransformer(applicationCodeTransformer)
    return
  }
  val frayContext = RunContext(config)
  // We only switch the runtime delegates when main thread is running. Otherwise we
  // may register system threads.
  val notifier =
      object : Delegate() {
        override fun onThreadRun() {
          if (Thread.currentThread().id == 1L) {
            val synchronizer = DelegateSynchronizer(frayContext)
            Runtime.NETWORK_DELEGATE = config.networkDelegateType.produce(frayContext, synchronizer)
            Runtime.TIME_DELEGATE = config.systemTimeDelegateType.produce(frayContext, synchronizer)
            Runtime.LOCK_DELEGATE = RuntimeDelegate(frayContext, synchronizer)
            Runtime.start()
          }
        }
      }
  Runtime.LOCK_DELEGATE = notifier
}
