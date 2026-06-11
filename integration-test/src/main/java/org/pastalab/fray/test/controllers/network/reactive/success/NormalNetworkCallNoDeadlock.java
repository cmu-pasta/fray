package org.pastalab.fray.test.controllers.network.reactive.success;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NormalNetworkCallNoDeadlock {
  public static void main(String[] args) throws InterruptedException, IOException {
    String host = "example.com";
    int port = 80;
    String request = "GET / HTTP/1.1\r\nHost: example.com\r\nConnection: close\r\n\r\n";

    Selector selector = Selector.open();
    SocketChannel channel = SocketChannel.open();
    channel.configureBlocking(false);
    channel.connect(new InetSocketAddress(host, port));
    channel.register(selector, SelectionKey.OP_CONNECT);

    ByteBuffer buffer = ByteBuffer.allocate(8192);
    boolean done = false;

    while (!done) {
      selector.select();
      Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
      while (keys.hasNext()) {
        SelectionKey key = keys.next();
        keys.remove();

        if (key.isConnectable()) {
          SocketChannel sc = (SocketChannel) key.channel();
          if (sc.finishConnect()) {
            sc.register(selector, SelectionKey.OP_WRITE);
          }
        } else if (key.isWritable()) {
          SocketChannel sc = (SocketChannel) key.channel();
          sc.write(ByteBuffer.wrap(request.getBytes()));
          sc.register(selector, SelectionKey.OP_READ);
        } else if (key.isReadable()) {
          SocketChannel sc = (SocketChannel) key.channel();
          int read = sc.read(buffer);
          if (read == -1) {
            done = true;
            sc.close();
          }
        }
      }
    }
    buffer.flip();
    selector.close();
  }
}
