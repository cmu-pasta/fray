package org.pastalab.fray.test.success.network;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

public class SyncServerSyncClient {
    private static final int PORT = 12345;
    private static final String SERVER_ADDRESS = "localhost";
    private static CountDownLatch latch;

    public static void main(String[] args) throws IOException, InterruptedException {
        latch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                runClient(0);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }).start();

        Thread serverThread = new Thread(() -> {
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
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(true);
        serverChannel.bind(new InetSocketAddress(PORT));
        latch.countDown();
        SocketChannel client = serverChannel.accept();
        serverChannel.close();
    }

    private static void runClient(int clientId) throws IOException, InterruptedException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(true);
        latch.await();
        channel.connect(new InetSocketAddress(SERVER_ADDRESS, PORT));
        channel.close();
    }
}
