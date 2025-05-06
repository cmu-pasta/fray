package org.pastalab.fray.core.concurrency.context

import java.nio.channels.SelectionKey
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.InterruptionType

class SelectorContext() : InterruptibleContext {
  val selectableChannelsToEventType = mutableMapOf<SelectableChannelContext, Int>()
  val waitingThreads = mutableListOf<ThreadContext>()

  fun setEventOp(selectableChannelContext: SelectableChannelContext, eventType: Int) {
    selectableChannelsToEventType[selectableChannelContext] = eventType
    selectableChannelContext.registeredSelectors.add(this)
  }

  fun select(threadContext: ThreadContext): Boolean {
    for ((context, type) in selectableChannelsToEventType) {
      if (context is ServerSocketChannelContext) {
        if (type and SelectionKey.OP_ACCEPT != 0 && context.pendingConnects > 0) {
          return false
        }
      }
      if (context is SocketChannelContext) {
        if (type and SelectionKey.OP_CONNECT != 0 &&
            context.channelReference.get()?.isOpen == true &&
            context.channelReference.get()?.remoteAddress != null) {
          return false
        }
        if (type and SelectionKey.OP_READ != 0 && context.pendingBytes != 0L) {
          return false
        }
      }
    }
    waitingThreads.add(threadContext)
    return true
  }

  fun cancel(context: SelectableChannelContext) {
    selectableChannelsToEventType.remove(context)
    context.registeredSelectors.remove(this)
  }

  override fun unblockThread(tid: Long, type: InterruptionType): Boolean {
    TODO("Not yet implemented")
  }
}
