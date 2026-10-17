package com.example.aijobs.application.dto;

import jakarta.validation.constraints.NotNull;

public record ApplicationRequest(@NotNull Long jobId, @NotNull Long resumeId) {
}
