package com.example.aijobs.evaluation;

import com.example.aijobs.deepseek.DeepSeekChatResult;

import java.math.BigDecimal;
import java.util.List;

public record ControlledComparisonResult(
        String scenarioId,
        String role,
        String jobRequirements,
        String resumeText,
        String inputHash,
        String ruleEvidenceHash,
        String modelPromptHash,
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
        String manualReviewStatus,
        List<String> manualReviewChecklist,
        List<String> contributionChecks,
        List<String> riskFlags) {
}
