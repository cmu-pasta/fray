package org.pastalab.fray.core.delegates

import java.net.SocketImpl
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import org.pastalab.fray.core.controllers.ReactiveNetworkController
import org.pastalab.fray.runtime.NetworkDelegate

class ReactiveNetworkDelegate(
    val controller: ReactiveNetworkController,
    val synchronizer: DelegateSynchronizer
) : NetworkDelegate() {

  fun reactiveBlockingEnter() {
    if (synchronizer.checkEntered()) {
      synchronizer.onSkipMethod("ReactiveBlocking")
      return
    }
    try {
      controller.reactiveBlockingBlocked()
    } finally {
      synchronizer.entered.set(false)
      synchronizer.onSkipMethod("ReactiveBlocking")
    }
  }

  fun reactiveBlockingEnterDone() {
    synchronizer.onSkipMethodDone("ReactiveBlocking")
    if (synchronizer.checkEntered()) return
    controller.reactiveBlockingUnblocked()
    synchronizer.entered.set(false)
  }

  override fun onSelectorSelect(selector: Selector) {
    reactiveBlockingEnter()
  }

  override fun onSelectorSelectDone(selector: Selector) {
    reactiveBlockingEnterDone()
  }

  override fun onServerSocketChannelAccept(channel: ServerSocketChannel) {
    reactiveBlockingEnter()
  }

  override fun onServerSocketChannelAcceptDone(
      channel: ServerSocketChannel,
      client: SocketChannel?
  ) {
    reactiveBlockingEnterDone()
  }

  override fun onSocketChannelRead(channel: SocketChannel) {
    reactiveBlockingEnter()
  }

  override fun onSocketChannelReadDone(channel: SocketChannel, bytesRead: Long) {
    reactiveBlockingEnterDone()
  }

  override fun onNioSocketConnect(socket: SocketImpl) {
    reactiveBlockingEnter()
  }

  override fun onNioSocketConnectDone(socket: SocketImpl) {
    reactiveBlockingEnterDone()
  }

  override fun onNioSocketRead(socket: SocketImpl) {
    reactiveBlockingEnter()
  }

  override fun onNioSocketReadDone(socket: SocketImpl, bytesRead: Int) {
    reactiveBlockingEnterDone()
  }
}
