package org.pastalab.fray.test.controllers.network.reactive.success.inputstream;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class PipedInputStreamReadNoDeadlock {
  public static void main(String[] args) throws Exception {
    try (PipedInputStream pipedInput = new PipedInputStream()) {
      PipedOutputStream output = new PipedOutputStream(pipedInput);
      output.write("Hello\n".getBytes());
      BufferedReader reader = new BufferedReader(new InputStreamReader(pipedInput));
      reader.readLine();
    }
  }
}
