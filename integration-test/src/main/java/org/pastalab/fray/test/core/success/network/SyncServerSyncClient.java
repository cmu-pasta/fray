package org.pastalab.fray.test.core.success.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

public class SyncServerSyncClient {
  private static final int PORT = 12345;
  private static final String SERVER_ADDRESS = "localhost";
  private static CountDownLatch latch;

  public static void main(String[] args) throws IOException, InterruptedException {
    latch = new CountDownLatch(1);
    new Thread(
            () -> {
              try {
                runClient(0);
              } catch (Throwable e) {
                throw new RuntimeException(e);
              }
            })
        .start();

    Thread serverThread =
        new Thread(
            () -> {
              try {
                runServer();
              } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
              }
            });
    serverThread.start();
  }

  private static void runServer() throws IOException, InterruptedException {
    ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.INET);
    serverChannel.configureBlocking(true);
    serverChannel.bind(new InetSocketAddress(PORT));
    latch.countDown();
    SocketChannel client = serverChannel.accept();
    client.configureBlocking(true);
    client.write(ByteBuffer.wrap("World".getBytes()));
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    client.read(buffer);
    buffer.flip();
    byte[] data = new byte[buffer.remaining()];
    buffer.get(data);
    String message = new String(data);
    System.out.println("Server received: " + message);
    serverChannel.close();
  }

  private static void runClient(int clientId) throws IOException, InterruptedException {
    SocketChannel channel = SocketChannel.open(StandardProtocolFamily.INET);
    channel.configureBlocking(true);
    latch.await();
    channel.connect(new InetSocketAddress(SERVER_ADDRESS, PORT));
    channel.write(ByteBuffer.wrap("Hello".getBytes()));
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    channel.read(buffer);
    buffer.flip();
    byte[] data = new byte[buffer.remaining()];
    buffer.get(data);
    String message = new String(data);
    System.out.println("Client received: " + message);
    channel.close();
  }
}
