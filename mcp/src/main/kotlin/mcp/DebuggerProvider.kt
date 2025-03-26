package mcp

interface DebuggerProvider {
  fun getLocalVariableValue(
    threadId: Long,
    className: String,
    methodName: String,
    lineNumber: Int,
    variableName: String,
    field: String?
  ): Result<String>
}
