package org.pastalab.fray.test.core.success.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
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
        ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.INET);
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(PORT));

        latch.countDown();

        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            SelectionKey key = keyIterator.next();
            assert(key.isAcceptable());
            ServerSocketChannel server = (ServerSocketChannel) key.channel();
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            keyIterator.remove();
            client.register(selector, SelectionKey.OP_READ);
        }
        {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            SelectionKey key = keyIterator.next();
            assert (key.isReadable());
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = client.read(buffer);
            buffer.flip();
            String receivedMessage = new String(buffer.array(), 0, bytesRead, StandardCharsets.UTF_8);
            System.out.println("Server received: " + receivedMessage);
            keyIterator.remove();
            client.register(selector, SelectionKey.OP_WRITE);
        }

        {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            SelectionKey key = keyIterator.next();
            assert (key.isWritable());
            SocketChannel client = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.wrap("World".getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                client.write(buffer);
            }
            System.out.println("Server sent: World");
            client.close();
        }
        serverChannel.close();
        selector.close();
    }

    private static void runClient(int clientId) throws IOException, InterruptedException {
        SocketChannel channel = SocketChannel.open(StandardProtocolFamily.INET);
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_CONNECT);
        latch.await();
        channel.connect(new InetSocketAddress(SERVER_ADDRESS, PORT));
        {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            SelectionKey key = keyIterator.next();
            assert(key.isConnectable());
            SocketChannel keyChannel = (SocketChannel) key.channel();
            if (keyChannel.isConnectionPending()) {
                keyChannel.finishConnect();
            }
            keyIterator.remove();
            keyChannel.register(selector, SelectionKey.OP_WRITE);
        }
        {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            SelectionKey key = keyIterator.next();
            assert (key.isWritable());
            SocketChannel keyChannel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.wrap("Hello".getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                keyChannel.write(buffer);
            }
            System.out.println("Client sent: Hello");
            keyIterator.remove();
            keyChannel.register(selector, SelectionKey.OP_READ);
        }

        {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            SelectionKey key = keyIterator.next();
            assert (key.isReadable());
            SocketChannel keyChannel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = keyChannel.read(buffer);
            buffer.flip();
            String receivedMessage = new String(buffer.array(), 0, bytesRead, StandardCharsets.UTF_8);
            System.out.println("Client received: " + receivedMessage);
            keyIterator.remove();
        }
        channel.close();
        selector.close();
    }
}
