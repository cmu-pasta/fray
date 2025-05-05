package org.pastalab.fray.core.concurrency.context

import java.lang.ref.WeakReference
import java.nio.channels.ServerSocketChannel
import org.pastalab.fray.core.ThreadContext

class ServerSocketChannelContext(channel: ServerSocketChannel) : SelectableChannelContext() {
  val channelReference = WeakReference(channel)
  var pendingConnects = 0
  var port = SERVER_PORT_NO_BIND
  var waitingThread: ThreadContext? = null

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
        waitingThread = thread
      }
      return channel.isBlocking
    }
    return false
  }

  companion object {
    const val SERVER_PORT_NO_BIND: Int = -1
  }
}
