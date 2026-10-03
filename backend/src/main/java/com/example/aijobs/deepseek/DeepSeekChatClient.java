package com.example.aijobs.deepseek;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.example.aijobs.deepseek.DeepSeekChatResult.Status.*;

public class DeepSeekChatClient implements AutoCloseable {
    private static final URI ENDPOINT = URI.create("https://api.deepseek.com/chat/completions");
    private static final int MAX_INPUT_CHARS = 24_000;
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024;
    private final DeepSeekProperties properties;
    private final ObjectMapper mapper;
    private final HttpClient http;
    private final URI endpoint;

    public DeepSeekChatClient(DeepSeekProperties properties, ObjectMapper mapper) {
        this(properties, mapper, HttpClient.newBuilder().connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build(), ENDPOINT);
    }

    DeepSeekChatClient(DeepSeekProperties properties, ObjectMapper mapper, HttpClient http, URI endpoint) {
        this.properties = properties;
        this.mapper = mapper;
        this.http = http;
        this.endpoint = endpoint;
    }

    public DeepSeekChatResult generate(List<DeepSeekMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("At least one text message is required");
        }
        var snapshot = List.copyOf(messages);
        if (snapshot.stream().mapToLong(message -> message.content().length()).sum() > MAX_INPUT_CHARS) {
            throw new IllegalArgumentException("Total message text exceeds 24000 characters");
        }
        if (!properties.enabled()) return fallback(DISABLED, null);
        if (properties.apiKey().isBlank()) return fallback(MISSING_API_KEY, null);

        CompletableFuture<HttpResponse<String>> pending = null;
        try {
            var payload = mapper.writeValueAsString(Map.of(
                    "model", properties.model(), "messages", snapshot,
                    "stream", false, "thinking", Map.of("type", "disabled"),
                    "max_tokens", properties.maxTokens()));
            var request = HttpRequest.newBuilder(endpoint).timeout(properties.requestTimeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
            pending = http.sendAsync(request, info -> new LimitedBodySubscriber());
            // Bound the entire body, including upstream keep-alive whitespace, to one deadline.
            var response = pending.get(properties.requestTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (response.statusCode() != 200) return fallback(UPSTREAM_ERROR, response.statusCode());
            if (!response.headers().firstValue("Content-Type").orElse("")
                    .toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
                return fallback(INVALID_RESPONSE, response.statusCode());
            }
            return parse(response.body());
        } catch (TimeoutException e) {
            if (pending != null) pending.cancel(true);
            return fallback(TIMEOUT, null);
        } catch (InterruptedException e) {
            if (pending != null) pending.cancel(true);
            Thread.currentThread().interrupt();
            return fallback(INTERRUPTED, null);
        } catch (ExecutionException e) {
            var status = e.getCause() instanceof HttpTimeoutException ? TIMEOUT
                    : e.getCause() instanceof ResponseLimitException ? INVALID_RESPONSE : NETWORK_ERROR;
            return fallback(status, null);
        } catch (IOException e) {
            return fallback(INVALID_RESPONSE, null);
        }
    }

    private DeepSeekChatResult parse(String payload) throws IOException {
        JsonNode root = mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(payload);
        if (root == null || !root.isObject() || !root.path("choices").isArray()
                || root.path("choices").isEmpty() || !root.path("model").isTextual()
                || root.path("model").asText().isBlank()) return fallback(INVALID_RESPONSE, 200);
        var choice = root.path("choices").get(0);
        var message = choice.path("message");
        var content = message.path("content");
        var finish = choice.path("finish_reason").asText("");
        if (!"assistant".equals(message.path("role").asText()) || !content.isTextual()
                || content.asText().isBlank() || (!"stop".equals(finish) && !"length".equals(finish))
                || message.hasNonNull("tool_calls")) return fallback(INVALID_RESPONSE, 200);
        var usageNode = root.path("usage");
        DeepSeekChatResult.Usage usage = null;
        if (!usageNode.isMissingNode() && !usageNode.isNull()) {
            if (!tokenCount(usageNode.path("prompt_tokens")) || !tokenCount(usageNode.path("completion_tokens"))
                    || !tokenCount(usageNode.path("total_tokens"))) return fallback(INVALID_RESPONSE, 200);
            if ((long) usageNode.path("prompt_tokens").intValue() + usageNode.path("completion_tokens").intValue()
                    != usageNode.path("total_tokens").intValue()) return fallback(INVALID_RESPONSE, 200);
            usage = new DeepSeekChatResult.Usage(usageNode.path("prompt_tokens").intValue(),
                    usageNode.path("completion_tokens").intValue(), usageNode.path("total_tokens").intValue());
        }
        return new DeepSeekChatResult(GENERATED, content.asText(), root.path("model").asText(), finish, usage, 200);
    }

    private boolean tokenCount(JsonNode value) {
        return value.isIntegralNumber() && value.canConvertToInt() && value.intValue() >= 0;
    }

    private DeepSeekChatResult fallback(DeepSeekChatResult.Status status, Integer httpStatus) {
        return DeepSeekChatResult.fallback(status, properties.model(), httpStatus);
    }

    @Override
    public void close() {
        http.close();
    }

    private static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<String> {
        private final HttpResponse.BodySubscriber<String> delegate =
                HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        private Flow.Subscription subscription;
        private long bytes;

        @Override
        public CompletionStage<String> getBody() {
            return delegate.getBody();
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            delegate.onSubscribe(subscription);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            bytes += buffers.stream().mapToLong(ByteBuffer::remaining).sum();
            if (bytes > MAX_RESPONSE_BYTES) {
                subscription.cancel();
                delegate.onError(new ResponseLimitException());
            } else {
                delegate.onNext(buffers);
            }
        }

        @Override
        public void onError(Throwable error) {
            delegate.onError(error);
        }

        @Override
        public void onComplete() {
            delegate.onComplete();
        }
    }

    private static final class ResponseLimitException extends IOException {
        private ResponseLimitException() {
            super("DeepSeek response exceeds the transport limit");
        }
    }
}
