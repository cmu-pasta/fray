package org.pastalab.fray.core.concurrency.context

import java.lang.ref.WeakReference
import java.nio.channels.SocketChannel
import org.pastalab.fray.core.ThreadContext
import org.pastalab.fray.core.concurrency.operations.ThreadResumeOperation
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.rmi.ThreadState
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey

open class SocketChannelContext(socketChannel: SocketChannel) : SelectableChannelContext() {
  val channelReference = WeakReference(socketChannel)
  var pendingBytes = 0L
  var waitingThreads = mutableListOf<ThreadContext>()

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
    verifyOrReport(pendingBytes > bytesRead) {
      "Pending bytes $pendingBytes is less than bytes read $bytesRead"
    }
    pendingBytes -= bytesRead
  }


  /**
   * Bytes received from remote peer. [bytesWritten] will be -1 if the remote peer is closed.
   */
  fun writeReceived(bytesWritten: Long) {
    if (bytesWritten == -1L) {
      pendingBytes = -1
    } else {
      pendingBytes += bytesWritten
    }
    if (channelReference.get()?.isBlocking == true) {
      for (thread in waitingThreads) {
        thread.state = ThreadState.Runnable
        thread.pendingOperation = ThreadResumeOperation(true)
      }
      waitingThreads.clear()
    } else {
      for (selectorContext in registeredSelectors) {
        if (selectorContext.selectableChannelsToEventType[this]?.and(SelectionKey.OP_READ) != 0) {
          for (context in selectorContext.waitingThreads) {
            context.state = ThreadState.Runnable
            context.pendingOperation = ThreadResumeOperation(true)
          }
          selectorContext.waitingThreads.clear()
        }
      }
    }
  }
}
