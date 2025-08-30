package org.pastalab.fray.test.controllers.network.reactive.fail.inputstream;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PipedInputStream;

public class PipedInputStreamReadDeadlock {
    public static void main(String[] args) throws Exception {
        try (PipedInputStream pipedInput = new PipedInputStream()) {
            pipedInput.read();
        }

    }
}
