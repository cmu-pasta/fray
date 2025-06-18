package org.pastalab.fray.test.controllers.network.reactive.success;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SimpleHttpServerHttpClient {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/test", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();

        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8000/test"))
                        .GET()
                        .build();
                try (var client = HttpClient.newHttpClient()) {
                    HttpResponse.BodyHandler<String> asString = HttpResponse.BodyHandlers.ofString();

                    HttpResponse<String> response = client.send(request, asString);

                    int statusCode = response.statusCode();
                    System.out.printf("Status Code: %s%n", statusCode);
                    HttpHeaders headers = response.headers();
                    System.out.printf("Response Headers: %s%n", headers);
                    System.out.println(response.body());
                    client.shutdown();
                    server.stop(0);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } catch (IOException e) {
            }
        }).start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            System.out.println(t.getRequestHeaders());
            String response = "This is the response";
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}
