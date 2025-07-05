package org.pastalab.fray.test.core.fail.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardProtocolFamily;
import java.nio.channels.ServerSocketChannel;

public class SyncServerAcceptDeadlock {
    public static final int PORT = 12345;

    public static void main(String[] args) throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.INET);
        serverChannel.configureBlocking(true);
        serverChannel.bind(new InetSocketAddress(PORT));
        serverChannel.accept();
    }
}
