package com.example.aijobs.evaluation;

import com.example.aijobs.deepseek.DeepSeekChatResult;

import java.math.BigDecimal;
import java.util.List;

public record ControlledComparisonResult(
        String scenarioId,
        String role,
        BigDecimal ruleScore,
        List<String> requiredSkills,
        List<String> matchedSkills,
        List<String> missingSkills,
        String ruleOnlySummary,
        String rulePlusModelSummary,
        boolean modelGenerated,
        DeepSeekChatResult.Status modelStatus,
        String modelName,
        Long latencyMs,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        List<String> contributionChecks,
        List<String> riskFlags) {
}
