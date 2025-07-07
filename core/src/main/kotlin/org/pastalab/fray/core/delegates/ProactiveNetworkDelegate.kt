package org.pastalab.fray.core.delegates

import java.net.SocketAddress
import java.net.SocketImpl
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.channels.spi.AbstractInterruptibleChannel
import org.pastalab.fray.core.controllers.ProactiveNetworkController
import org.pastalab.fray.core.utils.Utils.verifyOrReport
import org.pastalab.fray.runtime.NetworkDelegate

class ProactiveNetworkDelegate(
    val controller: ProactiveNetworkController,
    val synchronizer: DelegateSynchronizer
) : NetworkDelegate() {

  override fun onSelectorOpen() =
      synchronizer.runInFrayStart("Selector.open") { Result.success(Unit) }

  override fun onSelectorOpenDone() =
      synchronizer.runInFrayDone("Selector.open") { Result.success(Unit) }

  override fun onSelectorCancelKeyDone(selector: Selector, key: SelectionKey) =
      synchronizer.runInFrayDoneNoSkip { controller.selectorCancelKey(selector, key) }

  override fun onSelectorSelect(selector: Selector) =
      synchronizer.runInFrayStart("Selector.select") { controller.selectorSelect(selector) }

  override fun onSelectorSelectDone(selector: Selector) =
      synchronizer.runInFrayDone("Selector.select") { Result.success(Unit) }

  override fun onSelectorClose(selector: Selector) =
      synchronizer.runInFrayStart("Selector.close") { Result.success(Unit) }

  override fun onSelectorCloseDone(selector: Selector) =
      synchronizer.runInFrayDone("Selector.close") { controller.selectorClose(selector) }

  override fun onSelectorSetEventOpsDone(selector: Selector, key: SelectionKey) =
      synchronizer.runInFrayDoneNoSkip { controller.selectorSetEventOps(selector, key) }

  override fun onServerSocketChannelAccept(channel: ServerSocketChannel) =
      synchronizer.runInFrayStart("ServerSocketChannel.accept") {
        controller.serverSocketChannelAccept(channel)
      }

  override fun onServerSocketChannelAcceptDone(
      channel: ServerSocketChannel,
      client: SocketChannel?
  ) =
      synchronizer.runInFrayDone("ServerSocketChannel.accept") {
        controller.serverSocketChannelAcceptDone(channel, client)
      }

  override fun onSocketChannelRead(channel: SocketChannel) =
      synchronizer.runInFrayStart("SocketChannelRead") { controller.socketChannelRead(channel) }

  override fun onSocketChannelReadDone(channel: SocketChannel, bytesRead: Long) =
      synchronizer.runInFrayDone("SocketChannelRead") {
        controller.socketChannelReadDone(channel, bytesRead)
      }

  override fun onSocketChannelWriteDone(channel: SocketChannel, bytesWritten: Long) =
      synchronizer.runInFrayDoneNoSkip { controller.socketChannelWriteDone(channel, bytesWritten) }

  override fun onSocketChannelClose(channel: AbstractInterruptibleChannel?) =
      synchronizer.runInFrayStart("SocketChannel.close") { Result.success(Unit) }

  override fun onSocketChannelCloseDone(channel: AbstractInterruptibleChannel) =
      synchronizer.runInFrayDone("SocketChannel.close") {
        if (channel is ServerSocketChannel) {
          controller.serverSocketChannelClose(channel)
        } else if (channel is SocketChannel) {
          controller.socketChannelClose(channel)
        } else {
          verifyOrReport(false) {
            "Unknown channel type: ${channel::class.java.name}. Expected ServerSocketChannel or SocketChannel."
          }
          Result.success(Unit)
        }
      }

  override fun onSocketChannelConnect(channel: SocketChannel, remoteAddress: SocketAddress) =
      synchronizer.runInFrayStart("SocketChannel.connect") {
        controller.socketChannelConnect(channel, remoteAddress)
      }

  override fun onSocketChannelFinishConnect(channel: SocketChannel?) =
      synchronizer.runInFrayStart("SocketChannel.finishConnect") { Result.success(Unit) }

  override fun onSocketChannelFinishConnectDone(channel: SocketChannel, success: Boolean) =
      synchronizer.runInFrayDone("SocketChannel.finishConnect") {
        controller.socketChannelConnectDone(channel, success)
      }

  override fun onSocketChannelConnectDone(channel: SocketChannel, success: Boolean) =
      synchronizer.runInFrayDone("SocketChannel.connect") {
        controller.socketChannelConnectDone(channel, success)
      }

  override fun onServerSocketChannelBindDone(channel: ServerSocketChannel) =
      synchronizer.runInFrayDoneNoSkip { controller.serverSocketChannelBindDone(channel) }

  override fun onNioSocketConnect(socket: SocketImpl) {}

  override fun onNioSocketConnectDone(socket: SocketImpl) {}

  override fun onNioSocketRead(socket: SocketImpl) {}

  override fun onNioSocketReadDone(socket: SocketImpl, bytesRead: Int) {}
}
