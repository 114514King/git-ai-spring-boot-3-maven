package com.example.aijobs.match.dto;

import com.example.aijobs.match.entity.AiMatchResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MatchResponse(Long id, Long resumeId, Long jobId, BigDecimal score, String analysis,
                            String modelName, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static MatchResponse from(AiMatchResult result) {
        return new MatchResponse(result.getId(), result.getResumeId(), result.getJobId(), result.getScore(),
                result.getAnalysis(), result.getModelName(), result.getCreatedAt(), result.getUpdatedAt());
    }
}
