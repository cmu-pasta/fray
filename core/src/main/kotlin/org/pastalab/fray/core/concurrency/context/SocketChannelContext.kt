package org.pastalab.fray.core.concurrency.context

import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.context.ServerSocketChannelContext.Companion.SERVER_PORT_UNDEF
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.rmi.ThreadState

open class SocketChannelContext(socketChannel: SocketChannel?) : SelectableChannelContext() {
  var channelReference = WeakReference(socketChannel)
  var pendingBytes = 0L
  var waitingThreads = mutableListOf<ThreadContext>()
  var localPort = SERVER_PORT_UNDEF
  var remotePort = SERVER_PORT_UNDEF

  init {
    if (socketChannel?.isOpen == true) {
      localPort = (socketChannel.localAddress as? InetSocketAddress)?.port ?: SERVER_PORT_UNDEF
      remotePort = (socketChannel.remoteAddress as? InetSocketAddress)?.port ?: SERVER_PORT_UNDEF
    }
  }

  fun read(thread: ThreadContext): Boolean {
    if (pendingBytes != 0L) return false
    if (channelReference.get()?.isOpen != true) {
      return false
    }
    if (channelReference.get()?.isBlocking == true) {
      waitingThreads.add(thread)
      return true
    } else {
      return false
    }
  }

  fun readDone(bytesRead: Long) {
    if (pendingBytes == -1L) return
    verifyOrReport(pendingBytes >= bytesRead) {
      "Pending bytes $pendingBytes is less than bytes read $bytesRead"
    }
    pendingBytes -= bytesRead
  }

  fun unblockWaitingThreads() {
    for (thread in waitingThreads) {
      thread.state = ThreadState.Runnable
      thread.pendingOperation = ThreadResumeOperation(true)
    }
    waitingThreads.clear()
  }

  /** Bytes received from remote peer. [bytesWritten] will be -1 if the remote peer is closed. */
  fun writeReceived(bytesWritten: Long) {
    if (bytesWritten == -1L) {
      pendingBytes = -1
    } else {
      pendingBytes += bytesWritten
    }
    if (channelReference.get()?.isBlocking == true) {
      unblockWaitingThreads()
    } else {
      for (selectorContext in registeredSelectors) {
        if (selectorContext.selectableChannelsToEventType[this]?.and(SelectionKey.OP_READ) != 0) {
          selectorContext.unblockWaitingThreads()
        }
      }
    }
  }
}
