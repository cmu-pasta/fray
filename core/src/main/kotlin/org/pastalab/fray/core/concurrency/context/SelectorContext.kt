package org.pastalab.fray.core.concurrency.context

import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.InterruptionType

class SelectorContext(val selector: Selector) : InterruptibleContext {
  val selectableChannelsToEventType = mutableMapOf<SelectableChannelContext, Int>()
  val waitingThreads = mutableListOf<ThreadContext>()

  fun setEventOp(selectableChannelContext: SelectableChannelContext, eventType: Int) {
    selectableChannelsToEventType[selectableChannelContext] = eventType
    selectableChannelContext.registeredSelectors.add(this)
  }

  fun select(): Boolean {
    for ((context, type) in selectableChannelsToEventType) {
      if (context is ServerSocketChannelContext) {
        if (type and SelectionKey.OP_ACCEPT != 0 && context.pendingConnects > 0) {}
      }
    }
  }

  fun register() {}

  override fun unblockThread(tid: Long, type: InterruptionType): Boolean {
    TODO("Not yet implemented")
  }
}
