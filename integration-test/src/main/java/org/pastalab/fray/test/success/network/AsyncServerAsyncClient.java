package org.pastalab.fray.test.success.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class AsyncServerAsyncClient {
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
                throw new RuntimeException(e);
            }
        });
        serverThread.start();

    }

    private static void runServer() throws IOException, InterruptedException {
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(PORT));

        latch.countDown();

        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        selector.select();
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
        SelectionKey key = keyIterator.next();
        if (key.isAcceptable()) {
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();
            keyIterator.remove();
        }
        serverChannel.close();
        selector.close();
    }

    private static void runClient(int clientId) throws IOException, InterruptedException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_CONNECT);
        latch.await();
        channel.connect(new InetSocketAddress(SERVER_ADDRESS, PORT));
        selector.select();
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
        SelectionKey key = keyIterator.next();
        if (key.isConnectable()) {
            SocketChannel keyChannel = (SocketChannel) key.channel();
            if (keyChannel.isConnectionPending()) {
                keyChannel.finishConnect();
            }
        }
        channel.close();
        selector.close();
    }
}
