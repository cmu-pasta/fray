package org.pastalab.fray.core.concurrency.context

import java.lang.ref.WeakReference
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.rmi.ThreadState

class SocketChannelContext(socketChannel: SocketChannel) : SelectableChannelContext() {
  val channelReference = WeakReference(socketChannel)
  var hasPendingWrite = false
  var waitingThread: ThreadContext? = null

  fun connect(serverSocketChannelContext: ServerSocketChannelContext?) {
    if (serverSocketChannelContext == null) {
      return
    }
    if (channelReference.get()?.isConnected == true) {
      return
    }
    serverSocketChannelContext.pendingConnects += 1
    if (serverSocketChannelContext.channelReference.get()?.isBlocking == true) {
      val thread = serverSocketChannelContext.waitingThread
      if (thread != null) {
        thread.state = ThreadState.Runnable
        thread.pendingOperation = ThreadResumeOperation(true)
      }
    } else {
      for (selectorContext in serverSocketChannelContext.registeredSelectors) {
        verifyOrReport(
            selectorContext.selectableChannelsToEventType[serverSocketChannelContext] != null)
        // Server does not register the socket channel for OP_CONNECT, so we skip.
        if (selectorContext.selectableChannelsToEventType[serverSocketChannelContext]!! and
            SelectionKey.OP_ACCEPT == 0)
            continue
        for (context in selectorContext.waitingThreads) {
          context.state = ThreadState.Runnable
          context.pendingOperation = ThreadResumeOperation(true)
        }
        selectorContext.waitingThreads.clear()
      }
    }
  }
}
