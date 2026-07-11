package com.example.aijobs.admin.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminAiOperationInsight(
        String modelName,
        BigDecimal matchCoverageRate,
        long lowScoreMatches,
        String healthSummary,
        List<String> focusAreas,
        List<String> riskAlerts,
        List<String> suggestedActions) {
}
