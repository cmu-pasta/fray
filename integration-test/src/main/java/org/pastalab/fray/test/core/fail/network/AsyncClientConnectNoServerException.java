package org.pastalab.fray.test.core.fail.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class AsyncClientConnectNoServerException {
  private static final int PORT = 12345;
  private static final String SERVER_ADDRESS = "localhost";

  public static void main(String[] args) throws IOException, InterruptedException {
    runClient(0);
  }

  private static void runClient(int clientId) throws IOException, InterruptedException {
    SocketChannel channel = SocketChannel.open(StandardProtocolFamily.INET);
    channel.configureBlocking(false);
    Selector selector = Selector.open();
    try {

      channel.connect(new InetSocketAddress(SERVER_ADDRESS, PORT));
      channel.register(selector, SelectionKey.OP_CONNECT);
      selector.select();
      Set<SelectionKey> selectedKeys = selector.selectedKeys();
      Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
      SelectionKey key = keyIterator.next();
      if (key.isConnectable()) {
        SocketChannel keyChannel = (SocketChannel) key.channel();
        if (keyChannel.isConnectionPending()) {
          // Exception will be thrown here.
          keyChannel.finishConnect();
        }
        keyIterator.remove();
      }
    } finally {
      channel.close();
      selector.close();
    }
  }
}
