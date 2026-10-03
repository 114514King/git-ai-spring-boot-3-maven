package com.example.aijobs.deepseek;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("app.deepseek")
public record DeepSeekProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String apiKey,
        @DefaultValue("deepseek-flash") String model,
        @DefaultValue("PT3S") Duration connectTimeout,
        @DefaultValue("PT15S") Duration requestTimeout,
        @DefaultValue("512") int maxTokens) {

    public DeepSeekProperties {
        apiKey = apiKey == null ? "" : apiKey.strip();
        if (apiKey.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("app.deepseek.api-key contains control characters");
        }
        if (model == null || !model.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,99}")) {
            throw new IllegalArgumentException("app.deepseek.model must be a model identifier");
        }
        validateTimeout(connectTimeout, "connect-timeout");
        validateTimeout(requestTimeout, "request-timeout");
        if (maxTokens < 1 || maxTokens > 4096) {
            throw new IllegalArgumentException("app.deepseek.max-tokens must be between 1 and 4096");
        }
    }

    private static void validateTimeout(Duration timeout, String name) {
        if (timeout == null || timeout.compareTo(Duration.ofMillis(1)) < 0
                || timeout.compareTo(Duration.ofSeconds(60)) > 0) {
            throw new IllegalArgumentException("app.deepseek." + name + " must be between PT0.001S and PT60S");
        }
    }

    @Override
    public String toString() {
        return "DeepSeekProperties[enabled=" + enabled + ", apiKey=<redacted>, model=" + model
                + ", connectTimeout=" + connectTimeout + ", requestTimeout=" + requestTimeout
                + ", maxTokens=" + maxTokens + "]";
    }
}
