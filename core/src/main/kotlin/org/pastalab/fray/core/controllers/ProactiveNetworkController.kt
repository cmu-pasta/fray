package org.pastalab.fray.core.controllers

import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import org.pastalab.fray.core.RunContext
import org.pastalab.fray.core.concurrency.NioContextManager
import org.pastalab.fray.core.concurrency.operations.SelectorSelectBlocked
import org.pastalab.fray.core.concurrency.operations.SelectorSelectOperation
import org.pastalab.fray.core.concurrency.operations.ServerSocketChannelAcceptBlocked
import org.pastalab.fray.core.concurrency.operations.ServerSocketChannelAcceptOperation
import org.pastalab.fray.core.concurrency.operations.SocketChannelReadBlocked
import org.pastalab.fray.core.concurrency.operations.SocketChannelReadOperation
import org.pastalab.fray.rmi.ThreadState

class ProactiveNetworkController(val runContext: RunContext) : RunFinishedHandler(runContext) {
  val nioContextManager = NioContextManager()

  fun selectorSetEventOps(selector: Selector, key: SelectionKey) {
    val selectorContext = nioContextManager.getSelectorContext(selector)
    val channelContext = nioContextManager.getChannelContext(key.channel()) ?: return
    selectorContext.setEventOp(channelContext, key.interestOps())
  }

  fun selectorCancelKey(selector: Selector, key: SelectionKey) {
    val selectorContext = nioContextManager.getSelectorContext(selector)
    val channelContext = nioContextManager.getChannelContext(key.channel()) ?: return
    selectorContext.cancel(channelContext)
  }

  fun selectorSelect(selector: Selector) {
    val threadContext = runContext.registeredThreads[Thread.currentThread().id]!!
    val selectorContext = nioContextManager.getSelectorContext(selector)
    threadContext.state = ThreadState.Runnable
    threadContext.pendingOperation = SelectorSelectOperation(selectorContext)
    runContext.scheduleNextOperation(true)
    while (selectorContext.select(threadContext)) {
      threadContext.state = ThreadState.Blocked
      threadContext.pendingOperation = SelectorSelectBlocked(selectorContext)
      runContext.scheduleNextOperation(true)
    }
  }

  fun selectorClose(selector: Selector) {
    nioContextManager.selectorClose(selector)
  }

  fun serverSocketChannelBindDone(serverSocketChannel: ServerSocketChannel) {
    nioContextManager.serverSocketChannelBind(serverSocketChannel)
  }

  fun socketChannelClose(socketChannel: SocketChannel) {
    nioContextManager.socketChannelClose(socketChannel)
  }

  fun serverSocketChannelClose(serverSocketChannel: ServerSocketChannel) {
    nioContextManager.serverSocketChannelClose(serverSocketChannel)
  }

  fun socketChannelRead(socketChannel: SocketChannel) {
    val socketChannelContext = nioContextManager.getSocketChannelContext(socketChannel)
    val context = runContext.registeredThreads[Thread.currentThread().id]!!
    context.state = ThreadState.Runnable
    context.pendingOperation = SocketChannelReadOperation(socketChannelContext)
    runContext.scheduleNextOperation(true)
    while (socketChannelContext.read(context)) {
      context.state = ThreadState.Blocked
      context.pendingOperation = SocketChannelReadBlocked(socketChannelContext)
      runContext.scheduleNextOperation(true)
    }
  }

  fun socketChannelReadDone(socketChannel: SocketChannel, bytesRead: Long) {
    val socketChannelContext = nioContextManager.getSocketChannelContext(socketChannel)
    socketChannelContext.readDone(bytesRead)
  }

  fun serverSocketChannelAccept(serverSocketChannel: ServerSocketChannel) {
    val serverSocketChannelContext =
        nioContextManager.getServerSocketChannelContext(serverSocketChannel)
    val context = runContext.registeredThreads[Thread.currentThread().id]!!
    context.state = ThreadState.Runnable
    context.pendingOperation = ServerSocketChannelAcceptOperation(serverSocketChannelContext)
    runContext.scheduleNextOperation(true)
    while (serverSocketChannelContext.accept(context)) {
      context.state = ThreadState.Blocked
      context.pendingOperation = ServerSocketChannelAcceptBlocked(serverSocketChannelContext)
      runContext.scheduleNextOperation(true)
    }
    if (serverSocketChannelContext.pendingConnects > 0) {
      serverSocketChannelContext.pendingConnects--
    }
  }

  fun serverSocketChannelAcceptDone(
      serverSocketChannel: ServerSocketChannel,
      socketChannel: SocketChannel?
  ) {
    if (socketChannel != null) {
      nioContextManager.socketChannelAccepted(serverSocketChannel, socketChannel)
    }
  }

  fun socketChannelConnect(socketChannel: SocketChannel, address: SocketAddress) {
    if (address !is InetSocketAddress) {
      throw IllegalArgumentException("Only InetSocketAddress is supported for now.")
    }
    if (socketChannel.isConnected) {
      return
    }
    val serverSocketChannelContext =
        nioContextManager.getServerSocketChannelAtPort(address.port) ?: return
    serverSocketChannelContext.connectReceived()
  }

  fun socketChannelWriteDone(socketChannel: SocketChannel, bytesWritten: Long) {
    val port = (socketChannel.remoteAddress as? InetSocketAddress)?.port ?: return
    val localPort = (socketChannel.localAddress as? InetSocketAddress)?.port ?: return
    nioContextManager.getSocketChannelAtPort(port, localPort)?.writeReceived(bytesWritten)
  }

  fun socketChannelConnectDone(socketChannel: SocketChannel, success: Boolean) {
    if (success) {
      nioContextManager.socketChannelConnected(socketChannel)
    }
  }

  override fun done() {
    nioContextManager.done()
  }
}
