package org.pastalab.fray.test.controllers.network.reactive.success;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NetworkCallWithSocketNoDeadlock {
    public static void main(String[] args) throws IOException {
        String host = "example.com";
        int port = 80;
        String request = "GET / HTTP/1.1\r\nHost: example.com\r\nConnection: close\r\n\r\n";
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port));

            OutputStream out = socket.getOutputStream();
            out.write(request.getBytes());
            out.flush();

            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
            }
        }
    }
}
