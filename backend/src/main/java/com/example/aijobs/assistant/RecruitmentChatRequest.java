package com.example.aijobs.assistant;

import jakarta.validation.constraints.Size;

public record RecruitmentChatRequest(
        String conversationId,
        @Size(max = 1000, message = "消息不能超过 1000 字符") String message,
        Long applicationId,
        Long jobId,
        Long resumeId) {
}
