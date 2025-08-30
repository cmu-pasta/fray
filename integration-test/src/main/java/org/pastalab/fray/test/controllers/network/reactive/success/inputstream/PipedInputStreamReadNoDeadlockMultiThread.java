package org.pastalab.fray.test.controllers.network.reactive.success.inputstream;

import java.io.*;

public class PipedInputStreamReadNoDeadlockMultiThread {
    public static void main(String[] args) throws Exception {
        try (PipedInputStream pipedInput = new PipedInputStream()) {
            PipedOutputStream output = new PipedOutputStream(pipedInput);
            new Thread(() -> {
                try {
                    output.write("Hello\n".getBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(pipedInput));
            System.out.println("Read line: " + reader.readLine());

        }
    }
}
