package com.example.aijobs.resume.dto;

import jakarta.validation.constraints.NotNull;

public record ResumeOptimizationRequest(@NotNull(message = "岗位 ID 不能为空") Long jobId) {
}
