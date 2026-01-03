package org.pastalab.fray.core.concurrency

import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.nio.channels.SelectableChannel
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import org.pastalab.fray.core.concurrency.context.SelectableChannelContext
import org.pastalab.fray.core.concurrency.context.SelectorContext
import org.pastalab.fray.core.concurrency.context.ServerSocketChannelContext
import org.pastalab.fray.core.concurrency.context.ServerSocketChannelContext.Companion.SERVER_PORT_UNDEF
import org.pastalab.fray.core.concurrency.context.SocketChannelContext
import org.pastalab.fray.core.utils.Utils.verifyOrReport

class NioContextManager {
  private val selectorContextManager = ReferencedContextManager {
    verifyOrReport(it is Selector) { "Selector expected" }
    SelectorContext(it)
  }
  private val serverSocketChannelContextManager = ReferencedContextManager {
    verifyOrReport(it is ServerSocketChannel) { "ServerSocketChannel expected" }
    val serverSocketChannel = it as ServerSocketChannel
    val socketAddress = serverSocketChannel.localAddress
    val context = ServerSocketChannelContext(it)
    if (socketAddress is InetSocketAddress) {
      context.port = socketAddress.port
      portToServerSocketChannelContext[socketAddress.port] = context
    }
    context
  }
  private val socketChannelContextManager = ReferencedContextManager {
    verifyOrReport(it is SocketChannel) { "SocketChannel expected" }
    val socketChannel = it as SocketChannel
    val context = SocketChannelContext(socketChannel)
    context
  }
  /*
   * Connected sockets meaning sockets that are created by clients and connects to a server
   * through [SocketChannel.connect].
   */
  private val portToConnectedSockets: MutableMap<Int, SocketChannelContext> = mutableMapOf()
  private val portToServerSocketChannelContext: MutableMap<Int, ServerSocketChannelContext> =
      mutableMapOf()

  fun getChannelContext(channel: SelectableChannel): SelectableChannelContext? {
    return when (channel) {
      is SocketChannel -> {
        getSocketChannelContext(channel)
      }
      is ServerSocketChannel -> {
        getServerSocketChannelContext(channel)
      }
      else -> {
        null
      }
    }
  }

  fun getServerSocketChannelAtPort(port: Int): ServerSocketChannelContext? {
    return if (portToServerSocketChannelContext.containsKey(port)) {
      portToServerSocketChannelContext[port]
    } else {
      null
    }
  }

  fun getSocketChannelAtPort(port: Int, localPort: Int): SocketChannelContext? {
    if (portToConnectedSockets.containsKey(port)) {
      return portToConnectedSockets[port]
    } else if (portToServerSocketChannelContext.containsKey(port)) {
      val serverSocketChannelContext = portToServerSocketChannelContext[port]!!
      return serverSocketChannelContext.getOrCreateSocketContextAtPort(localPort)
    }
    return null
  }

  fun getSocketChannelContext(channel: SocketChannel): SocketChannelContext {
    return socketChannelContextManager.getContext(channel)
  }

  fun getSelectorContext(selector: Selector): SelectorContext {
    return selectorContextManager.getContext(selector)
  }

  fun getServerSocketChannelContext(channel: ServerSocketChannel): ServerSocketChannelContext {
    return serverSocketChannelContextManager.getContext(channel)
  }

  fun socketChannelAccepted(
      serverSocketChannel: ServerSocketChannel,
      socketChannel: SocketChannel,
  ) {
    val serverSocketChannelContext = getServerSocketChannelContext(serverSocketChannel)
    val socketChannelContext =
        serverSocketChannelContext.acceptedSocketChannels.firstOrNull {
          it.remotePort == (socketChannel.remoteAddress as? InetSocketAddress)?.port
        }
    // This port has received write operation before accept. So we need to re-initialize it.
    if (socketChannelContext != null) {
      socketChannelContext.localPort =
          (socketChannel.localAddress as? InetSocketAddress)?.port ?: SERVER_PORT_UNDEF
      socketChannelContext.channelReference = WeakReference(socketChannel)
      socketChannelContextManager.addContext(socketChannel, socketChannelContext)
    } else {
      val newContext = getSocketChannelContext(socketChannel)
      serverSocketChannelContext.acceptedSocketChannels.add(newContext)
    }
  }

  fun socketChannelConnected(channel: SocketChannel) {
    val context = socketChannelContextManager.getContext(channel)
    val port = (channel.localAddress as? InetSocketAddress?)?.port ?: return
    context.remotePort = (channel.remoteAddress as? InetSocketAddress)?.port ?: SERVER_PORT_UNDEF
    context.localPort = port
    verifyOrReport(!portToConnectedSockets.containsKey(port)) {
      "Socket channel is already connected to port $port"
    }
    portToConnectedSockets[port] = context
  }

  fun serverSocketChannelBind(channel: ServerSocketChannel) {
    val context = serverSocketChannelContextManager.getContext(channel)
    val newPort = (channel.localAddress as? InetSocketAddress?)?.port ?: -1
    verifyOrReport(context.port != SERVER_PORT_UNDEF) {
      "Server socket channel is already bound to port ${context.port}"
    }
    if (newPort == context.port) {
      return
    }
    verifyOrReport(!portToServerSocketChannelContext.containsKey(newPort)) {
      "Port $newPort is already bound to another server socket channel"
    }
    context.port = newPort
    portToServerSocketChannelContext[newPort] = context
  }

  fun selectorClose(selector: Selector) {
    val selectorContext = selectorContextManager.getContext(selector)
    selectorContext.unblockWaitingThreads()
  }

  fun serverSocketChannelClose(channel: ServerSocketChannel) {
    val context = serverSocketChannelContextManager.getContext(channel)
    context.unblockWaitingThreads()
    if (context.port != SERVER_PORT_UNDEF) {
      portToServerSocketChannelContext.remove(context.port)
      context.port = SERVER_PORT_UNDEF
    }
  }

  fun socketChannelClose(channel: SocketChannel) {
    val context = socketChannelContextManager.getContext(channel)
    context.unblockWaitingThreads()
    val remotePort = context.remotePort
    val localPort = context.localPort
    // This channel is initiated by the server socket channel.
    if (portToConnectedSockets.containsKey(remotePort)) {
      // So we need to notify the client that the connection is closed.
      portToConnectedSockets[remotePort]?.writeReceived(-1L)
      portToServerSocketChannelContext[localPort]?.acceptedSocketChannels?.removeIf {
        it.remotePort == remotePort
      }
    }
    // This channel is initiated by the client.
    if (portToServerSocketChannelContext.containsKey(remotePort)) {
      // So we need to notify the server that the connection is closed.
      val serverSocketChannel = portToServerSocketChannelContext[remotePort]!!
      serverSocketChannel.getOrCreateSocketContextAtPort(localPort).writeReceived(-1L)
    }
    portToConnectedSockets.remove(localPort)
  }

  fun done() {
    portToServerSocketChannelContext.values.forEach { it.channelReference.get()?.close() }
    serverSocketChannelContextManager.objMap.values.forEach { it.channelReference.get()?.close() }
    socketChannelContextManager.objMap.values.forEach { it.channelReference.get()?.close() }
    portToServerSocketChannelContext.clear()
    selectorContextManager.objMap.values.forEach { it.selectorReference.get()?.close() }
    portToConnectedSockets.values.forEach { it.channelReference.get()?.close() }
    portToConnectedSockets.clear()
  }
}
