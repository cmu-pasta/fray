package org.pastalab.fray.test.success.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SelectorTest {
    private static final int PORT = 12345;
    private static final String SERVER_ADDRESS = "localhost";
    
    public static void main(String[] args) throws IOException, InterruptedException {
        Thread serverThread = new Thread(() -> {
            try {
                runServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        
        for (int i = 0; i < 3; i++) {
            final int clientId = i;
            new Thread(() -> {
                try {
                    runClient(clientId);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
    
    private static void runServer() throws IOException {
        Selector selector = Selector.open();
        
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(PORT));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        System.out.println("Server started on port " + PORT);
        
        ByteBuffer buffer = ByteBuffer.allocate(256);
        int messageReceived = 0;
        
        while (messageReceived < 3) {
            selector.select();
            
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                
                if (key.isAcceptable()) {
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    SocketChannel client = server.accept();
                    client.configureBlocking(false);
                    client.register(selector, SelectionKey.OP_READ);
                    System.out.println("New client connected: " + client.getRemoteAddress());
                } else if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    buffer.clear();
                    int bytesRead = client.read(buffer);
                    
                    if (bytesRead == -1) {
                        client.close();
                        key.cancel();
                    } else {
                        buffer.flip();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        String message = new String(bytes);
                        System.out.println("Received from client: " + message);
                        messageReceived += 1;
                        buffer.flip();
                        client.write(buffer);
                    }
                }
                keyIterator.remove();
            }
        }
        serverChannel.close();
    }
    
    private static void runClient(int clientId) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(true); // Clients use blocking mode for simplicity
        
        channel.connect(new InetSocketAddress(SERVER_ADDRESS, PORT));
        System.out.println("Client " + clientId + " connected to server");
        
        String message = "Hello from client " + clientId;
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        channel.write(buffer);
        
        buffer.clear();
        channel.read(buffer);
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        System.out.println("Client " + clientId + " received: " + new String(bytes));
        
        channel.close();
    }
}
