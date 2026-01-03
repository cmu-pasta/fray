package org.pastalab.fray.test.core.fail.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.SocketChannel;

public class SyncClientExceptionWithoutServer {
  private static final int PORT = 12345;

  public static void main(String[] args) throws IOException, InterruptedException {
    runClient(0);
  }

  private static void runClient(int clientId) throws IOException, InterruptedException {
    SocketChannel channel = SocketChannel.open(StandardProtocolFamily.INET);
    channel.configureBlocking(true); // Clients use blocking mode for simplicity
    channel.connect(new InetSocketAddress("localhost", PORT));
    channel.close();
  }
}
