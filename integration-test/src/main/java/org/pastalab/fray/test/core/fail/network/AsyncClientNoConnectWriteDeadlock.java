package org.pastalab.fray.test.core.fail.network;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class AsyncClientNoConnectWriteDeadlock {
    public static void main(String[] args) throws IOException, InterruptedException {
        SocketChannel channel = SocketChannel.open(StandardProtocolFamily.INET);
        channel.configureBlocking(false);
        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_WRITE);
        selector.select();
        selector.select();
    }
}