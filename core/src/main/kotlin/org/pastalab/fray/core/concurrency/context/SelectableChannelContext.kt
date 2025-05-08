package org.pastalab.fray.core.concurrency.context

abstract class SelectableChannelContext {
  var registeredSelectors = mutableSetOf<SelectorContext>()
}
