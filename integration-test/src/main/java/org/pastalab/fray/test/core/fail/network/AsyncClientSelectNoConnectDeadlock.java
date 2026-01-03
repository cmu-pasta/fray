package org.pastalab.fray.test.core.fail.network;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class AsyncClientSelectNoConnectDeadlock {
  public static void main(String[] args) throws IOException, InterruptedException {
    runClient(0);
  }

  private static void runClient(int clientId) throws IOException, InterruptedException {
    SocketChannel channel = SocketChannel.open(StandardProtocolFamily.INET);
    Selector selector = Selector.open();
    try {
      channel.configureBlocking(false);
      channel.register(selector, SelectionKey.OP_CONNECT);
      selector.select();
    } finally {
      channel.close();
      selector.close();
    }
  }
}
