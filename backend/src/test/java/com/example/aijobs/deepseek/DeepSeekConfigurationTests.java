package com.example.aijobs.deepseek;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class DeepSeekConfigurationTests {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(DeepSeekConfiguration.class)
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void defaultsRemainDisabledWithNoKey() {
        runner.run(context -> {
            assertNull(context.getStartupFailure());
            var properties = context.getBean(DeepSeekProperties.class);
            assertFalse(properties.enabled());
            assertEquals("", properties.apiKey());
            assertEquals("deepseek-flash", properties.model());
            assertEquals(Duration.ofSeconds(3), properties.connectTimeout());
            assertEquals(Duration.ofSeconds(15), properties.requestTimeout());
            assertEquals(512, properties.maxTokens());
            assertNotNull(context.getBean(DeepSeekChatClient.class));
        });
    }

    @Test
    void bindsExplicitConfigurationAndRedactsKey() {
        runner.withPropertyValues("app.deepseek.enabled=true", "app.deepseek.api-key=synthetic-key",
                "app.deepseek.model=deepseek-v4-pro", "app.deepseek.connect-timeout=PT2S",
                "app.deepseek.request-timeout=PT20S", "app.deepseek.max-tokens=256").run(context -> {
            assertNull(context.getStartupFailure());
            var properties = context.getBean(DeepSeekProperties.class);
            assertTrue(properties.enabled());
            assertEquals("deepseek-v4-pro", properties.model());
            assertEquals(Duration.ofSeconds(2), properties.connectTimeout());
            assertEquals(Duration.ofSeconds(20), properties.requestTimeout());
            assertEquals(256, properties.maxTokens());
            assertFalse(properties.toString().contains("synthetic-key"));
        });
    }

    @Test
    void rejectsInvalidTimeoutsBudgetAndHeaderInjection() {
        assertThrows(IllegalArgumentException.class, () -> properties("key", Duration.ZERO, 512));
        assertThrows(IllegalArgumentException.class, () -> properties("key", Duration.ofSeconds(61), 512));
        assertThrows(IllegalArgumentException.class, () -> properties("key", Duration.ofSeconds(1), 0));
        assertThrows(IllegalArgumentException.class, () -> properties("key", Duration.ofSeconds(1), 4097));
        assertThrows(IllegalArgumentException.class, () -> properties("key\r\nInjected: header", Duration.ofSeconds(1), 512));
    }

    private DeepSeekProperties properties(String key, Duration timeout, int maxTokens) {
        return new DeepSeekProperties(true, key, "deepseek-flash", timeout, timeout, maxTokens);
    }
}
