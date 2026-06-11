package org.pastalab.fray.runtime;

import java.io.PipedInputStream;
import java.net.SocketAddress;
import java.net.SocketImpl;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractInterruptibleChannel;

public class NetworkDelegate {

  public void onSelectorOpen() {}

  public void onSelectorOpenDone() {}

  public void onSelectorSetEventOpsDone(Selector selector, SelectionKey key) {}

  public void onSelectorCancelKeyDone(Selector selector, SelectionKey key) {}

  public void onSelectorSelect(Selector selector) {}

  public void onSelectorClose(Selector selector) {}

  public void onSelectorCloseDone(Selector selector) {}

  public void onSelectorSelectDone(Selector selector) {}

  public void onServerSocketChannelBindDone(ServerSocketChannel channel) {}

  public void onServerSocketChannelAccept(ServerSocketChannel channel) {}

  public void onServerSocketChannelAcceptDone(ServerSocketChannel channel, SocketChannel client) {}

  public void onSocketChannelClose(AbstractInterruptibleChannel channel) {}

  public void onSocketChannelCloseDone(AbstractInterruptibleChannel channel) {}

  public void onSocketChannelConnect(SocketChannel channel, SocketAddress remoteAddress) {}

  public void onSocketChannelConnectDone(SocketChannel channel, boolean success) {}

  public void onSocketChannelFinishConnect(SocketChannel channel) {}

  public void onSocketChannelFinishConnectDone(SocketChannel channel, boolean success) {}

  public void onSocketChannelRead(SocketChannel channel) {}

  public void onSocketChannelReadDone(SocketChannel channel, long bytesRead) {}

  public void onSocketChannelWriteDone(SocketChannel channel, long bytesWritten) {}

  public void onNioSocketConnect(SocketImpl socket) {}

  public void onNioSocketConnectDone(SocketImpl socket) {}

  public void onNioSocketRead(SocketImpl socket) {}

  public void onNioSocketReadDone(SocketImpl socket, int bytesRead) {}

  public void onNioSocketAccept(SocketImpl socket) {}

  public void onNioSocketAcceptDone(SocketImpl socket) {}

  public void onPipedInputStreamRead(PipedInputStream reader) {}

  public void onPipedInputStreamReadDone(PipedInputStream reader) {}
}
