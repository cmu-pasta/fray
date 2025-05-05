package org.pastalab.fray.test.fail.network;

import java.io.IOException;
import java.nio.channels.Selector;

public class SelectorWithNoRegistrationDeadlock {
    public static void main(String[] args) throws IOException, InterruptedException {
        Selector selector = Selector.open();
        try {
            selector.select();
        } finally {
            selector.close();
        }
    }
}
