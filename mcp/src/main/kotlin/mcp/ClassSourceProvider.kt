package mcp

interface ClassSourceProvider {
  fun getClassSource(className: String): String?
}
