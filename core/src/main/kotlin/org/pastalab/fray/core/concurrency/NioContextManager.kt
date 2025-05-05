package org.pastalab.fray.core.concurrency

import java.net.InetSocketAddress
import java.nio.channels.SelectableChannel
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import org.pastalab.fray.core.concurrency.context.SelectableChannelContext
import org.pastalab.fray.core.concurrency.context.SelectorContext
import org.pastalab.fray.core.concurrency.context.ServerSocketChannelContext
import org.pastalab.fray.core.concurrency.context.ServerSocketChannelContext.Companion.SERVER_PORT_NO_BIND
import org.pastalab.fray.core.concurrency.context.SocketChannelContext
import org.pastalab.fray.core.utils.Utils.verifyOrReport

class NioContextManager {
  private val selectorContextManager =
      ReferencedContextManager<SelectorContext> {
        verifyOrReport(it is Selector) { "Selector expected" }
        SelectorContext()
      }
  private val serverSocketChannelContextManager =
      ReferencedContextManager<ServerSocketChannelContext> {
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
  private val socketChannelContextManager =
      ReferencedContextManager<SocketChannelContext> {
        verifyOrReport(it is SocketChannel) { "SocketChannel expected" }
        val socketChannel = it as SocketChannel
        val context = SocketChannelContext(socketChannel)
        context
      }
  private val portToSocketChannelContext: MutableMap<Int, SocketChannelContext> = mutableMapOf()
  private val portToServerSocketChannelContext: MutableMap<Int, ServerSocketChannelContext> =
      mutableMapOf()

  fun getChannelContext(channel: SelectableChannel): SelectableChannelContext? {
    return if (channel is SocketChannel) {
      getSocketChannelContext(channel)
    } else if (channel is ServerSocketChannel) {
      getServerSocketChannelContext(channel)
    } else {
      null
    }
  }

  fun getServerSocketChannelAtPort(port: Int): ServerSocketChannelContext? {
    if (portToServerSocketChannelContext.containsKey(port)) {
      return portToServerSocketChannelContext[port]
    } else {
      return null
    }
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

  fun serverSocketChannelBind(channel: ServerSocketChannel) {
    val context = serverSocketChannelContextManager.getContext(channel)
    val newPort = (channel.localAddress as InetSocketAddress?)?.port ?: -1
    verifyOrReport(context.port != SERVER_PORT_NO_BIND) {
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

  fun serverSocketChannelClose(channel: SocketChannel) {
    if (serverSocketChannelContextManager.hasContext(channel)) {
      val context = serverSocketChannelContextManager.getContext(channel)
      if (context.port != SERVER_PORT_NO_BIND) {
        portToServerSocketChannelContext.remove(context.port)
        context.port = SERVER_PORT_NO_BIND
      }
    }
  }

  fun done() {
    portToServerSocketChannelContext.values.forEach { it.channelReference.get()?.close() }
    serverSocketChannelContextManager.objMap.values.forEach {
      it.first.channelReference.get()?.close()
    }
    socketChannelContextManager.objMap.values.forEach { it.first.channelReference.get()?.close() }
    portToServerSocketChannelContext.clear()
  }
}
