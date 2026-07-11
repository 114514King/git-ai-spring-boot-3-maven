package com.example.aijobs.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record StudentApplicationActionPlanResponse(
        Long applicationId,
        Long jobId,
        Long resumeId,
        String applicationStatus,
        String modelName,
        BigDecimal matchScore,
        String scoreSource,
        String priorityLevel,
        String statusSummary,
        List<String> preparationChecklist,
        List<String> riskReminders,
        List<String> nextActions
) {
}
