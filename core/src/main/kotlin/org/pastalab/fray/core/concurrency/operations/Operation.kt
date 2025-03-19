package org.anonlab.fray.core.concurrency.operations

abstract class Operation {
  override fun toString(): String {
    return this.javaClass.simpleName
  }
}
