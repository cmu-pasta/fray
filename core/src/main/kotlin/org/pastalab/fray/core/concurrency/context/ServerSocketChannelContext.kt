package org.pastalab.fray.core.concurrency.context

import java.lang.ref.WeakReference
import java.nio.channels.ServerSocketChannel
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.rmi.ThreadState
import java.nio.channels.SelectionKey

class ServerSocketChannelContext(channel: ServerSocketChannel) : SelectableChannelContext() {
  val channelReference = WeakReference(channel)
  // This list stores all socket channels created by server through the accept method.
  val acceptedSocketChannels = mutableListOf<SocketChannelContext>()
  var pendingConnects = 0
  var port = SERVER_PORT_NO_BIND
  var waitingThreads = mutableListOf<ThreadContext>()

  /**
   * Determine if this operation will block the current thread. If multiple clients are trying to
   * connect to the server socket channel, this method will randomly choose one of them to be
   * connected.
   *
   * @return true if this operation will block the curren thread.
   */
  fun accept(thread: ThreadContext): Boolean {
    val channel = channelReference.get() ?: return false
    if (!channel.isOpen) {
      return false
    }
    if (port == SERVER_PORT_NO_BIND) {
      // if the server socket channel is not bound, we do nothing since the
      // following call will fail.
      return false
    }
    if (pendingConnects == 0) {
      if (channel.isBlocking) {
        waitingThreads.add(thread)
      }
      return channel.isBlocking
    }
    return false
  }

  fun connectReceived() {
    pendingConnects += 1
    if (channelReference.get()?.isBlocking == true) {
      for (thread in waitingThreads) {
        thread.state = ThreadState.Runnable
        thread.pendingOperation = ThreadResumeOperation(true)
      }
      waitingThreads.clear()
    } else {
      for (selectorContext in registeredSelectors) {
        verifyOrReport(
            selectorContext.selectableChannelsToEventType[this] != null)
        // Server does not register the socket channel for OP_CONNECT, so we skip.
        if (selectorContext.selectableChannelsToEventType[this]!! and
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

  companion object {
    const val SERVER_PORT_NO_BIND: Int = -1
  }
}
