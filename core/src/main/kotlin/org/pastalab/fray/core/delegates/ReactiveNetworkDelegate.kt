package org.pastalab.fray.core.delegates

import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import org.pastalab.fray.core.controllers.ReactiveNetworkController
import org.pastalab.fray.runtime.NetworkDelegate

class ReactiveNetworkDelegate(
    val controller: ReactiveNetworkController,
    val synchronizer: DelegateSynchronizer
) : NetworkDelegate() {

  override fun onSelectorSelect(selector: Selector) {
    if (synchronizer.checkEntered()) {
      synchronizer.onSkipMethod("Selector.select")
      return
    }
    try {
      controller.selectorBlocked(selector)
    } finally {
      synchronizer.entered.set(false)
      synchronizer.onSkipMethod("Selector.select")
    }
  }

  override fun onSelectorSelectDone(selector: Selector) {
    synchronizer.onSkipMethodDone("Selector.select")
    if (synchronizer.checkEntered()) return
    controller.selectorSelectDone()
    synchronizer.entered.set(false)
  }

  override fun onServerSocketChannelAccept(channel: ServerSocketChannel) {
    if (synchronizer.checkEntered()) {
      synchronizer.onSkipMethod("ServerSocketChannel.accept")
      return
    }
    try {
      controller.socketChannelBlocked(channel)
    } finally {
      synchronizer.entered.set(false)
      synchronizer.onSkipMethod("ServerSocketChannel.accept")
    }
  }

  override fun onServerSocketChannelAcceptDone(
      channel: ServerSocketChannel,
      client: SocketChannel?
  ) {
    synchronizer.onSkipMethodDone("ServerSocketChannel.accept")
    if (synchronizer.checkEntered()) return
    try {
      controller.socketChannelBlockedDone()
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onSocketChannelRead(channel: SocketChannel) {
    if (synchronizer.checkEntered()) {
      synchronizer.onSkipMethod("SocketChannel.read")
      return
    }
    try {
      controller.socketChannelBlocked(channel)
    } finally {
      synchronizer.entered.set(false)
      synchronizer.onSkipMethod("SocketChannel.read")
    }
  }

  override fun onSocketChannelReadDone(channel: SocketChannel, bytesRead: Long) {
    synchronizer.onSkipMethodDone("SocketChannel.read")
    if (synchronizer.checkEntered()) return
    try {
      controller.socketChannelBlockedDone()
    } finally {
      synchronizer.entered.set(false)
    }
  }
}
