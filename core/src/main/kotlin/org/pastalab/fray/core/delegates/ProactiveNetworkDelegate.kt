package org.pastalab.fray.core.delegates

import java.net.SocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.channels.spi.AbstractInterruptibleChannel
import org.pastalab.fray.core.controllers.ProactiveNetworkController
import org.pastalab.fray.runtime.NetworkDelegate

class ProactiveNetworkDelegate(
    val controller: ProactiveNetworkController,
    val synchronizer: DelegateSynchronizer
) : NetworkDelegate() {
  override fun onSelectorCancelKeyDone(selector: Selector, key: SelectionKey) {
    if (synchronizer.checkEntered()) {
      return
    }
    try {
      controller.selectorCancelKey(selector, key)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onSelectorSelect(selector: Selector) {
    if (synchronizer.checkEntered()) {
      synchronizer.onSkipMethod("Selector.select")
      return
    }
    try {
      controller.selectorSelect(selector)
    } finally {
      synchronizer.entered.set(false)
      synchronizer.onSkipMethod("Selector.select")
    }
  }

  override fun onSelectorSelectDone(selector: Selector?) {
    synchronizer.onSkipMethodDone("Selector.select")
  }

  override fun onSelectorClose(selector: Selector?) {
    synchronizer.onSkipMethod("Selector.close")
  }

  override fun onSelectorCloseDone(selector: Selector) {
    synchronizer.onSkipMethodDone("Selector.close")
    if (synchronizer.checkEntered()) return
    controller.selectorClose(selector)
    synchronizer.entered.set(false)
  }

  override fun onSelectorSetEventOpsDone(selector: Selector, key: SelectionKey) {
    if (synchronizer.checkEntered()) {
      return
    }
    try {
      controller.selectorSetEventOps(selector, key)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onServerSocketChannelAccept(channel: ServerSocketChannel) {
    if (synchronizer.checkEntered()) {
      synchronizer.onSkipMethod("ServerSocketChannel.accept")
      return
    }
    try {
      controller.serverSocketChannelAccept(channel)
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
      controller.serverSocketChannelAcceptDone(channel, client)
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
      controller.socketChannelRead(channel)
    } finally {
      synchronizer.entered.set(false)
      synchronizer.onSkipMethod("SocketChannel.read")
    }
  }

  override fun onSocketChannelReadDone(channel: SocketChannel, bytesRead: Long) {
    synchronizer.onSkipMethodDone("SocketChannel.read")
    if (synchronizer.checkEntered()) return
    try {
      controller.socketChannelReadDone(channel, bytesRead)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onSocketChannelWriteDone(channel: SocketChannel, bytesWritten: Long) {
    if (synchronizer.checkEntered()) return
    try {
      controller.socketChannelWriteDone(channel, bytesWritten)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onSocketChannelClose(channel: AbstractInterruptibleChannel?) {
    synchronizer.onSkipMethod("SocketChannel.close")
  }

  override fun onSocketChannelCloseDone(channel: AbstractInterruptibleChannel) {
    synchronizer.onSkipMethodDone("SocketChannel.close")
    if (synchronizer.checkEntered()) return
    if (channel is ServerSocketChannel) {
      controller.serverSocketChannelClose(channel)
    } else if (channel is SocketChannel) {
      controller.socketChannelClose(channel)
    }
    synchronizer.entered.set(false)
  }

  override fun onSocketChannelConnect(channel: SocketChannel, remoteAddress: SocketAddress) {
    if (synchronizer.checkEntered()) {
      synchronizer.onSkipMethod("SocketChannel.connect")
      return
    }
    try {
      controller.socketChannelConnect(channel, remoteAddress)
    } finally {
      synchronizer.entered.set(false)
      synchronizer.onSkipMethod("SocketChannel.connect")
    }
  }

  override fun onSocketChannelFinishConnect(channel: SocketChannel?) {
    synchronizer.onSkipMethod("SocketChannel.finishConnect")
  }

  override fun onSocketChannelFinishConnectDone(channel: SocketChannel, success: Boolean) {
    synchronizer.onSkipMethodDone("SocketChannel.finishConnect")
    if (synchronizer.checkEntered()) return
    try {
      controller.socketChannelConnectDone(channel, success)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onSocketChannelConnectDone(channel: SocketChannel, success: Boolean) {
    synchronizer.onSkipMethodDone("SocketChannel.connect")
    if (synchronizer.checkEntered()) return
    try {
      controller.socketChannelConnectDone(channel, success)
    } finally {
      synchronizer.entered.set(false)
    }
  }

  override fun onServerSocketChannelBindDone(channel: ServerSocketChannel) {
    if (synchronizer.checkEntered()) return
    controller.serverSocketChannelBindDone(channel)
    synchronizer.entered.set(false)
  }
}
