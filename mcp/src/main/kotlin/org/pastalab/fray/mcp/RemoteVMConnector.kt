package org.pastalab.fray.mcp

import com.sun.jdi.ArrayReference
import com.sun.jdi.ArrayType
import com.sun.jdi.Bootstrap
import com.sun.jdi.ObjectReference
import com.sun.jdi.StackFrame
import com.sun.jdi.ThreadReference
import com.sun.jdi.Value

fun removeVMConnectorWithHostAndPort(hostAndPort: String): RemoteVMConnector {
  val (hostname, port) = hostAndPort.split(":")
//  return RemoteVMConnector(hostname, port.toInt())
  val connectors = Bootstrap.virtualMachineManager().attachingConnectors()
  val connector = connectors.first { it.name() == "com.sun.jdi.SocketAttach" }
  val arguments = connector.defaultArguments()
  arguments["hostname"]?.setValue(hostname)
  arguments["port"]?.setValue(port.toString())
  val vm = connector.attach(arguments)
  return RemoteVMConnector(object: VirtualMachineProxy{
    override fun allThreads(): List<ThreadReference> {
      return vm.allThreads()
    }
  })
}

class RemoteVMConnector(val vm: VirtualMachineProxy): DebuggerProvider {

  override fun getLocalVariableValue(
      threadId: Long,
      className: String,
      methodName: String,
      lineNumber: Int,
      variableName: String,
      field: String?
  ): Result<String> {
    return runCatching {
      val thread = vm.allThreads().find { it.uniqueID() == threadId }

      requireNotNull(thread) { "The thread with ID $threadId is not found." }

      return@runCatching if (!thread.isSuspended) {
        thread.suspend()
        try {
          findAndGetVariables(thread, className, methodName, lineNumber, variableName, field)
        } finally {
          thread.resume()
        }
      } else {
        findAndGetVariables(thread, className, methodName, lineNumber, variableName, field)
      }
    }
  }

  private fun findAndGetVariables(
      thread: ThreadReference,
      className: String,
      methodName: String,
      lineNumber: Int,
      variableName: String,
      field: String?
  ): String {
    for (i in 0 until thread.frameCount()) {
      val frame = thread.frame(i)
      val location = frame.location()
      val frameMethod = location.method()
      val frameClass = frameMethod.declaringType().name()
      if ((className == frameClass || frameClass.endsWith(".$className")) &&
          methodName == frameMethod.name() &&
          (lineNumber == -1 || lineNumber == location.lineNumber())) {
        return getFrameVariables(frame, variableName, field)
      }
    }
    throw RuntimeException("No frame found for $className.$methodName:$lineNumber")
  }

  fun getFrameVariables(frame: StackFrame, variableName: String, field: String?): String {
    val visibleVariables = frame.visibleVariables()
    val thisObj = frame.thisObject()
    if (thisObj != null && variableName == "this") {
      return if (field != null) {
        fromObjectField(thisObj, field)
      } else {
        formatValue(frame.thisObject())
      }
    } else {
      for (variable in visibleVariables) {
        if (variableName == variable.name()) {
          val value = frame.getValue(variable)
          if (field != null) {
            require(value is ObjectReference) { "Field access is only supported for objects." }
            return fromObjectField(value, field)
          } else {
            return formatValue(value)
          }
        }
      }
    }
    throw RuntimeException("No variable found for $variableName")
  }

  private fun fromObjectField(obj: ObjectReference, field: String): String {
    obj.getValue(obj.referenceType().fieldByName(field)).let {
      return formatValue(it)
    }
  }

  private fun formatValue(value: Value?): String {
    if (value == null) return "null"
    return when {
      value.type().name().startsWith("java.lang.") -> value.toString()
      (value.type() is ArrayType) -> {
        val arrayRef = value as ArrayReference
        val size = arrayRef.length()
        val elements = (0 until size).map { i -> formatValue(arrayRef.getValue(i)) }
        "[${elements.joinToString(", ")}]"
      }
      else -> "${value.type().name()}@${Integer.toHexString(value.hashCode())}"
    }
  }
}
