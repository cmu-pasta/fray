package org.pastalab.fray.mcp

import com.sun.jdi.ThreadReference

interface VirtualMachineProxy {
  fun allThreads(): List<ThreadReference>
}
