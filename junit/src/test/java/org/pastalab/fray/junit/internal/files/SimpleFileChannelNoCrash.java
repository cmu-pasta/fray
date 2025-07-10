package org.pastalab.fray.junit.internal.files;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.pastalab.fray.junit.junit5.FrayTestExtension;
import org.pastalab.fray.junit.junit5.annotations.ConcurrencyTest;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Files;

@ExtendWith(FrayTestExtension.class)
public class SimpleFileChannelNoCrash {

    @TempDir
    Path testFolder;

    private Path testFilePath;

    @BeforeEach
    public void setup() throws IOException {
        testFilePath = testFolder.resolve("test.in");
        String content = "Hello, Fray testing with FileChannel!";
        Files.write(testFilePath, content.getBytes());
    }

    @AfterEach
    public void cleanup() throws IOException {
        if (Files.exists(testFilePath)) {
            Files.deleteIfExists(testFilePath);
        }
    }

    @ConcurrencyTest(iterations = 100)
    public void testFileChannel() throws IOException {
        RandomAccessFile reader = new RandomAccessFile(testFilePath.toFile(), "r");
        FileChannel fc = reader.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = fc.read(buffer);
        buffer.flip();
        fc.close(); // Fray does not support this, but we're testing it doesn't crash
        reader.close();
    }
}
