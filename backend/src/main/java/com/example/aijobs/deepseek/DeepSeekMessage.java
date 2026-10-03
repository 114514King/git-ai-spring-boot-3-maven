package com.example.aijobs.deepseek;

import java.util.Set;

public record DeepSeekMessage(String role, String content) {
    public DeepSeekMessage {
        if (role == null || !Set.of("system", "user", "assistant").contains(role)) {
            throw new IllegalArgumentException("Only system, user and assistant text messages are supported");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content must not be blank");
        }
    }
}
