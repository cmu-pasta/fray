package org.pastalab.fray.mcp

interface ClassSourceProvider {
  fun getClassSource(className: String): String?
}
