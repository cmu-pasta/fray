package org.pastalab.fray.test.core.fail.network;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SimpleHttpClient {
    public static void main(String[] args) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://example.com"))
                .header("X-App", "Demo")
                .GET()
                .build();
        var client = HttpClient.newHttpClient();

        HttpResponse.BodyHandler<String> asString = HttpResponse.BodyHandlers.ofString();

        HttpResponse<String> response = client.send(request, asString);

        int statusCode = response.statusCode();
        System.out.printf("Status Code: %s%n", statusCode);
        HttpHeaders headers = response.headers();
        System.out.printf("Response Headers: %s%n", headers);
        System.out.println(response.body());
        client.shutdown();
    }
}
