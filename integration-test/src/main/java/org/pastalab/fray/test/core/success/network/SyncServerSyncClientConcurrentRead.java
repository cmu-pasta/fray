package org.pastalab.fray.test.core.success.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class SyncServerSyncClientConcurrentRead {
    private static final int PORT = 12345;
    private static final String SERVER_ADDRESS = "localhost";
    private static final String MESSAGE = "Hello World";
    private static CountDownLatch latch;

    public static void main(String[] args) throws IOException, InterruptedException {
        latch = new CountDownLatch(1);
        Thread clientThread = new Thread(() -> {
            try {
                runClient(0);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
        clientThread.start();
        Thread serverThread = new Thread(() -> {
            try {
                runServer();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
        serverThread.start();
        System.out.println("Test completed successfully!");
    }

    private static void runServer() throws IOException, InterruptedException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(true);
        serverChannel.bind(new InetSocketAddress(PORT));
        latch.countDown();
        SocketChannel client = serverChannel.accept();
        ByteBuffer buffer = ByteBuffer.wrap(MESSAGE.getBytes(StandardCharsets.UTF_8));
        while (buffer.hasRemaining()) {
            client.write(buffer);
        }
        System.out.println("Server sent: " + MESSAGE);
        client.close();
        serverChannel.close();
    }

    private static void runClient(int clientId) throws IOException, InterruptedException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(true);
        latch.await();
        channel.connect(new InetSocketAddress(SERVER_ADDRESS, PORT));

        {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = channel.read(buffer);
            buffer.flip();
            String receivedMessage = new String(buffer.array(), 0, bytesRead, StandardCharsets.UTF_8);
            System.out.println("Client received: " + receivedMessage);
        }
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                int bytesRead = 0;
                try {
                    bytesRead = channel.read(buffer);
                } catch (IOException e) {
                }
                if (bytesRead > 0) {
                    buffer.flip();
                    String receivedMessage = new String(buffer.array(), 0, bytesRead, StandardCharsets.UTF_8);
                    System.out.println("Client received: " + receivedMessage);
                }
            }).start();
        }
        channel.close();
    }
}