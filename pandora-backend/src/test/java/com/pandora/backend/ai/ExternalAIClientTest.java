package com.pandora.backend.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ExternalAIClientTest {
    @Test void sendsRealHttpRequestToNormalizedChatPathWithConfiguredModel() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> body = new AtomicReference<>(), authorization = new AtomicReference<>();
        server.createContext("/v1/chat/completions", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"fixture answer\"}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length); exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
            assertEquals("fixture answer", new ExternalAIClient().call("fixture prompt", "MBTI_REPORT", url, "fixture-key", 3000, "fixture-model"));
            assertEquals("Bearer fixture-key", authorization.get());
            assertTrue(body.get().contains("fixture-model"));
            assertTrue(body.get().contains("fixture prompt"));
        } finally { server.stop(0); }
    }
}
