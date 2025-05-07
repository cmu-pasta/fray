package org.pastalab.fray.test.success.network;

import java.io.IOException;
import java.nio.channels.Selector;

public class AsyncSelectorAsyncClose {
    public static void main(String[] args) throws IOException {
        Selector selector = Selector.open();
        new Thread(() -> {
            try {
                selector.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        selector.select();
    }
}
