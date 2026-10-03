package com.example.aijobs.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.aijobs.deepseek.DeepSeekChatResult.Status.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeepSeekChatClientTests {
    private static final List<DeepSeekMessage> MESSAGES = List.of(
            new DeepSeekMessage("system", "Explain synthetic teaching skills. Human review is required."),
            new DeepSeekMessage("user", "Synthetic candidate: English and lesson planning."),
            new DeepSeekMessage("assistant", "Please confirm the synthetic job requirements."),
            new DeepSeekMessage("user", "English, lesson planning, classroom management. \u82f1\u8bed"));
    private static final String RESPONSE = """
            {"model":"deepseek-flash","choices":[{"index":0,"message":{"role":"assistant",
            "content":"Verify classroom management evidence with a human reviewer."},"finish_reason":"stop"}],
            "usage":{"prompt_tokens":42,"completion_tokens":12,"total_tokens":54}}
            """;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<HttpHandler> handler = new AtomicReference<>();
    private final AtomicReference<JsonNode> requestBody = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> method = new AtomicReference<>();
    private final AtomicInteger requests = new AtomicInteger();
    private final CountDownLatch release = new CountDownLatch(1);
    private HttpServer server;
    private ExecutorService executor;
    private URI endpoint;

    @BeforeEach
    void startSyntheticUpstream() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        handler.set(exchange -> reply(exchange, 200, RESPONSE));
        server.createContext("/chat/completions", exchange -> {
            requests.incrementAndGet();
            method.set(exchange.getRequestMethod());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(mapper.readTree(exchange.getRequestBody()));
            try (exchange) {
                handler.get().handle(exchange);
            }
        });
        server.start();
        endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions");
    }

    @AfterEach
    void stopSyntheticUpstream() {
        release.countDown();
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    void disabledMakesNoHttpRequest() {
        try (var client = client(properties(false, "test-key", Duration.ofSeconds(3)))) {
            assertFallback(client.generate(MESSAGES), DISABLED);
            assertEquals(0, requests.get());
        }
    }

    @Test
    void missingKeyMakesNoHttpRequest() {
        try (var client = client(properties(true, "  ", Duration.ofSeconds(3)))) {
            assertFallback(client.generate(MESSAGES), MISSING_API_KEY);
            assertEquals(0, requests.get());
        }
    }

    @Test
    void sendsAuthenticatedNonStreamingTextContractAndMapsUsage() {
        try (var client = client(properties(true, "synthetic-test-key", Duration.ofSeconds(3)))) {
            var result = client.generate(MESSAGES);

            assertTrue(result.generated(), result.status().name());
            assertEquals("deepseek-flash", result.model());
            assertEquals("Verify classroom management evidence with a human reviewer.", result.content());
            assertEquals(new DeepSeekChatResult.Usage(42, 12, 54), result.usage());
            assertFalse(result.truncated());
            assertEquals("POST", method.get());
            assertEquals("Bearer synthetic-test-key", authorization.get());
            var request = requestBody.get();
            assertEquals("deepseek-flash", request.path("model").asText());
            assertEquals(512, request.path("max_tokens").asInt());
            assertFalse(request.path("stream").asBoolean(true));
            assertEquals("disabled", request.path("thinking").path("type").asText());
            assertEquals(mapper.valueToTree(MESSAGES), request.path("messages"));
            assertFalse(request.has("tools"));
            assertEquals(1, requests.get());
        }
    }

    @Test
    void honorsConfiguredModelAndOutputBudgetAndReportsActualModel() {
        var properties = new DeepSeekProperties(true, "test-key", "deepseek-v4-pro",
                Duration.ofSeconds(1), Duration.ofSeconds(3), 256);
        try (var client = client(properties)) {
            assertEquals("deepseek-flash", client.generate(MESSAGES).model());
            assertEquals("deepseek-v4-pro", requestBody.get().path("model").asText());
            assertEquals(256, requestBody.get().path("max_tokens").asInt());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {301, 401, 429, 500, 503})
    void unsuccessfulStatusIsExplicitWithoutErrorBodyOrRetries(int status) {
        handler.set(exchange -> {
            exchange.getResponseHeaders().set("Location", endpoint.toString());
            reply(exchange, status, "{\"error\":\"sensitive upstream diagnostics\"}");
        });
        try (var client = client(properties(true, "test-key", Duration.ofSeconds(3)))) {
            var result = client.generate(MESSAGES);
            assertFallback(result, UPSTREAM_ERROR);
            assertEquals(status, result.httpStatus());
            assertFalse(result.toString().contains("sensitive"));
            assertEquals(1, requests.get());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"not json", "null", "{}", "{\"model\":\"deepseek-flash\",\"choices\":[]}",
            "{\"model\":\"deepseek-flash\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\" \"},\"finish_reason\":\"stop\"}]}",
            "{\"model\":\"deepseek-flash\",\"choices\":[{\"message\":{\"role\":\"user\",\"content\":\"unsafe\"},\"finish_reason\":\"stop\"}]}",
            "{\"model\":\"deepseek-flash\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"unsafe\"},\"finish_reason\":\"tool_calls\"}]}",
            "{\"model\":\"deepseek-flash\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"unsafe\",\"tool_calls\":[]},\"finish_reason\":\"stop\"}]}"})
    void invalidOrNonTextEnvelopesAreNotAcceptedAsGeneratedText(String body) {
        handler.set(exchange -> reply(exchange, 200, body));
        try (var client = client(properties(true, "test-key", Duration.ofSeconds(3)))) {
            assertFallback(client.generate(MESSAGES), INVALID_RESPONSE);
        }
    }

    @Test
    void truncatedOutputRetainsExplicitFinishReason() {
        handler.set(exchange -> reply(exchange, 200, RESPONSE.replace("\"stop\"", "\"length\"")));
        try (var client = client(properties(true, "test-key", Duration.ofSeconds(3)))) {
            var result = client.generate(MESSAGES);
            assertTrue(result.generated());
            assertTrue(result.truncated());
            assertEquals("length", result.finishReason());
        }
    }

    @Test
    void invalidUsageMetadataIsRejected() {
        handler.set(exchange -> reply(exchange, 200, RESPONSE.replace("\"prompt_tokens\":42", "\"prompt_tokens\":-1")));
        try (var client = client(properties(true, "test-key", Duration.ofSeconds(3)))) {
            assertFallback(client.generate(MESSAGES), INVALID_RESPONSE);
        }
    }

    @Test
    void trailingJsonIsNotAcceptedAsGeneratedText() {
        handler.set(exchange -> reply(exchange, 200, RESPONSE + "{\"score\":100}"));
        try (var client = client(properties(true, "test-key", Duration.ofSeconds(3)))) {
            assertFallback(client.generate(MESSAGES), INVALID_RESPONSE);
        }
    }

    @Test
    void inconsistentTokenTotalsAreRejected() {
        handler.set(exchange -> reply(exchange, 200, RESPONSE.replace("\"total_tokens\":54", "\"total_tokens\":53")));
        try (var client = client(properties(true, "test-key", Duration.ofSeconds(3)))) {
            assertFallback(client.generate(MESSAGES), INVALID_RESPONSE);
        }
    }

    @Test
    void nonJsonMediaTypeIsRejected() {
        handler.set(exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, RESPONSE.getBytes(StandardCharsets.UTF_8).length);
            exchange.getResponseBody().write(RESPONSE.getBytes(StandardCharsets.UTF_8));
        });
        try (var client = client(properties(true, "test-key", Duration.ofSeconds(3)))) {
            assertFallback(client.generate(MESSAGES), INVALID_RESPONSE);
        }
    }

    @Test
    void responseLimitPreventsUnboundedUpstreamBody() {
        handler.set(exchange -> reply(exchange, 200, " ".repeat(1024 * 1024 + 1)));
        try (var client = client(properties(true, "test-key", Duration.ofSeconds(3)))) {
            assertFallback(client.generate(MESSAGES), INVALID_RESPONSE);
        }
    }

    @Test
    void delayedHeadersTimeOut() {
        handler.set(exchange -> {
            awaitRelease();
            reply(exchange, 200, RESPONSE);
        });
        try (var client = client(properties(true, "test-key", Duration.ofMillis(250)))) {
            assertFallback(client.generate(MESSAGES), TIMEOUT);
        } finally {
            release.countDown();
        }
    }

    @Test
    void deadlineIncludesIncompleteBodyDespiteUpstreamKeepAlives() {
        handler.set(exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            try {
                while (!release.await(30, TimeUnit.MILLISECONDS)) {
                    exchange.getResponseBody().write(' ');
                    exchange.getResponseBody().flush();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        try (var client = client(properties(true, "test-key", Duration.ofMillis(400)))) {
            var started = System.nanoTime();
            assertFallback(client.generate(MESSAGES), TIMEOUT);
            assertTrue(Duration.ofNanos(System.nanoTime() - started).compareTo(Duration.ofSeconds(3)) < 0);
        } finally {
            release.countDown();
        }
    }

    @Test
    void networkFailureDoesNotLeakExceptionMessage() {
        var http = mock(HttpClient.class);
        when(http.sendAsync(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.failedFuture(new ConnectException("sensitive request details")));
        try (var client = new DeepSeekChatClient(properties(true, "test-key", Duration.ofSeconds(3)), mapper, http, endpoint)) {
            var result = client.generate(MESSAGES);
            assertFallback(result, NETWORK_ERROR);
            assertFalse(result.toString().contains("sensitive"));
        }
    }

    @Test
    void interruptionCancelsRequestAndPreservesInterruptFlag() {
        var http = mock(HttpClient.class);
        var pending = new CompletableFuture<HttpResponse<String>>();
        when(http.sendAsync(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(pending);
        try (var client = new DeepSeekChatClient(properties(true, "test-key", Duration.ofSeconds(3)), mapper, http, endpoint)) {
            Thread.currentThread().interrupt();
            assertFallback(client.generate(MESSAGES), INTERRUPTED);
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue(pending.isCancelled());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void invalidOrOversizedInternalMessagesMakeNoHttpRequest() {
        try (var client = client(properties(true, "test-key", Duration.ofSeconds(3)))) {
            assertThrows(IllegalArgumentException.class, () -> new DeepSeekMessage("tool", "execute"));
            assertThrows(IllegalArgumentException.class, () -> new DeepSeekMessage("user", " "));
            assertThrows(IllegalArgumentException.class, () -> client.generate(List.of()));
            assertThrows(IllegalArgumentException.class, () -> client.generate(null));
            assertThrows(IllegalArgumentException.class,
                    () -> client.generate(List.of(new DeepSeekMessage("user", "x".repeat(24_001)))));
            assertEquals(0, requests.get());
        }
    }

    private DeepSeekChatClient client(DeepSeekProperties properties) {
        var http = HttpClient.newBuilder().connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return new DeepSeekChatClient(properties, mapper, http, endpoint);
    }

    private DeepSeekProperties properties(boolean enabled, String key, Duration timeout) {
        return new DeepSeekProperties(enabled, key, "deepseek-flash", Duration.ofSeconds(1), timeout, 512);
    }

    private void reply(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private void awaitRelease() {
        try {
            release.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void assertFallback(DeepSeekChatResult result, DeepSeekChatResult.Status status) {
        assertEquals(status, result.status());
        assertFalse(result.generated());
        assertFalse(result.truncated());
        assertEquals("", result.content());
        assertNull(result.finishReason());
        assertNull(result.usage());
    }
}
