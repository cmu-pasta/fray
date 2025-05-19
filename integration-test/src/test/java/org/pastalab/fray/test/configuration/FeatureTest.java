package org.pastalab.fray.test.configuration;

import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.pastalab.fray.core.TestRunner;
import org.pastalab.fray.core.command.Configuration;
import org.pastalab.fray.core.command.ExecutionInfo;
import org.pastalab.fray.core.command.InterceptedFeatures;
import org.pastalab.fray.core.command.LambdaExecutor;
import org.pastalab.fray.core.randomness.ControlledRandom;
import org.pastalab.fray.core.scheduler.POSScheduler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;

public class FeatureTest {

    public void setHttpRequest() {
        SocketChannel socketChannel = null;
        try {
            URI uri = new URI("http://example.com");
            String host = uri.getHost();
            int port = uri.getPort() == -1 ? 80 : uri.getPort();
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(host, port));
            String httpRequest = "GET / HTTP/1.1\r\n" +
                    "Host: " + host + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
            ByteBuffer requestBuffer = ByteBuffer.wrap(httpRequest.getBytes(StandardCharsets.UTF_8));
            socketChannel.write(requestBuffer);
            ByteBuffer responseBuffer = ByteBuffer.allocate(8192);
            StringBuilder response = new StringBuilder();
            while (socketChannel.read(responseBuffer) != -1) {
                responseBuffer.flip();
                response.append(StandardCharsets.UTF_8.decode(responseBuffer));
                responseBuffer.clear();
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        } finally {
            if (socketChannel != null && socketChannel.isOpen()) {
                try {
                    socketChannel.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Test
    public void testNetworkFeatureDisabledNoDeadlock() throws IOException {
        Path workDir = Files.createTempDirectory("feature-test-temp");
        HashSet<InterceptedFeatures> features = new HashSet<>(InterceptedFeatures.getEntries());
        features.remove(InterceptedFeatures.NETWORK);
        Configuration config = new Configuration(
                new ExecutionInfo(
                        new LambdaExecutor(() -> {
                            setHttpRequest();
                            return null;
                        }),
                        false,
                        false,
                        -1
                ),
                workDir.toString(),
                1000,
                60,
                new POSScheduler(),
                new ControlledRandom(),
                true,
                false,
                true,
                false,
                false,
                false,
                features
        );
        TestRunner runner = new TestRunner(config);
        assertNull(runner.run());
    }
}
