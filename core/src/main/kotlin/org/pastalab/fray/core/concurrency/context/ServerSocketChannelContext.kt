package org.pastalab.fray.core.concurrency.context

import java.lang.ref.WeakReference
import java.nio.channels.SelectionKey
import java.nio.channels.ServerSocketChannel
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.rmi.ThreadState

class ServerSocketChannelContext(channel: ServerSocketChannel) : SelectableChannelContext() {
  val channelReference = WeakReference(channel)
  // This list stores all socket channels created by server through the accept method.
  val acceptedSocketChannels = mutableListOf<SocketChannelContext>()
  var pendingConnects = 0
  var port = (channel.localAddress as? java.net.InetSocketAddress)?.port ?: SERVER_PORT_UNDEF
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
    if (port == SERVER_PORT_UNDEF) {
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

  /*
   * This method returns a socket channel context given a port number.
   * The server may receive bytes even if the server did not call accept. So
   * we need to track these socket channels manually.
   */
  fun getOrCreateSocketContextAtPort(port: Int): SocketChannelContext {
    for (socketChannelContext in acceptedSocketChannels) {
      if (socketChannelContext.remotePort == port) {
        return socketChannelContext
      }
    }
    val socketChannelContext = SocketChannelContext(null)
    socketChannelContext.remotePort = port
    acceptedSocketChannels.add(socketChannelContext)
    return socketChannelContext
  }

  fun unblockWaitingThreads() {
    for (thread in waitingThreads) {
      thread.state = ThreadState.Runnable
      thread.pendingOperation = ThreadResumeOperation(true)
    }
    waitingThreads.clear()
  }

  fun connectReceived() {
    pendingConnects += 1
    if (channelReference.get()?.isBlocking == true) {
      unblockWaitingThreads()
    } else {
      for (selectorContext in registeredSelectors) {
        verifyOrReport { selectorContext.selectableChannelsToEventType[this] != null }
        // Server does not register the socket channel for OP_CONNECT, so we skip.
        if (selectorContext.selectableChannelsToEventType[this]!! and SelectionKey.OP_ACCEPT == 0)
            continue
        selectorContext.unblockWaitingThreads()
      }
    }
  }

  companion object {
    const val SERVER_PORT_UNDEF: Int = -1
  }
}
