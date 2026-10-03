package com.example.aijobs.deepseek;

public record DeepSeekChatResult(Status status, String content, String model,
                                 String finishReason, Usage usage, Integer httpStatus) {
    public enum Status {
        GENERATED, DISABLED, MISSING_API_KEY, TIMEOUT, UPSTREAM_ERROR,
        NETWORK_ERROR, INVALID_RESPONSE, INTERRUPTED
    }

    public record Usage(int promptTokens, int completionTokens, int totalTokens) {}

    public boolean generated() {
        return status == Status.GENERATED;
    }

    public boolean truncated() {
        return generated() && "length".equals(finishReason);
    }

    static DeepSeekChatResult fallback(Status status, String model, Integer httpStatus) {
        return new DeepSeekChatResult(status, "", model, null, null, httpStatus);
    }
}
