package org.pastalab.fray.mcp

import com.sun.jdi.Bootstrap
import com.sun.jdi.ThreadReference
import com.sun.jdi.VirtualMachine

class StandaloneMCPServerDebugger : VirtualMachineProxy {
  val vm: VirtualMachine

  override fun allThreads(): List<ThreadReference> {
    return vm.allThreads()
  }

  init {
    val connector =
        Bootstrap.virtualMachineManager().attachingConnectors().firstOrNull {
          it.name().contains("SocketAttach")
        } ?: throw RuntimeException("Could not find dt_socket connector")
    val args = connector.defaultArguments()
    args["hostname"]?.setValue("localhost")
    args["port"]?.setValue("5005")
    vm = connector.attach(args)
    vm.resume()
  }
}
