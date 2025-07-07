package org.pastalab.fray.test.core.fail.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class AsyncClientSelectAfterCloseDeadlock {
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
        ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.INET);
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(PORT));

        latch.countDown();
        try {
            System.out.println("Server started on port " + PORT);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            SelectionKey key = keyIterator.next();
            if (key.isAcceptable()) {
                ServerSocketChannel server = (ServerSocketChannel) key.channel();
                SocketChannel client = server.accept();
                System.out.println("New client connected: " + client.getRemoteAddress());
                keyIterator.remove();
            }
        } finally {
            serverChannel.close();
            selector.close();
        }

    }

    private static void runClient(int clientId) throws IOException, InterruptedException {
        SocketChannel channel = SocketChannel.open(StandardProtocolFamily.INET);
        Selector selector = Selector.open();
        try {

            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress(SERVER_ADDRESS, PORT));
            channel.register(selector, SelectionKey.OP_CONNECT);
            latch.await();
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            SelectionKey key = keyIterator.next();
            if (key.isConnectable()) {
                SocketChannel keyChannel = (SocketChannel) key.channel();
                if (keyChannel.isConnectionPending()) {
                    keyChannel.finishConnect();
                }
                keyIterator.remove();
            }
            channel.close();
            selector.select();
        } finally {
            selector.close();
        }
    }
}
