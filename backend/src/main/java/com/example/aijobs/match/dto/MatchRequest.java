package com.example.aijobs.match.dto;

import jakarta.validation.constraints.NotNull;

public record MatchRequest(@NotNull Long resumeId, @NotNull Long jobId) {
}
