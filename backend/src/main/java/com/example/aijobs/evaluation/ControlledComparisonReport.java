package com.example.aijobs.evaluation;

import java.util.List;

public record ControlledComparisonReport(
        String evaluationId,
        String scenarioSetVersion,
        String scenarioSetHash,
        String runMode,
        String ruleModelName,
        String dictionaryVersion,
        int scenarioCount,
        int generatedCount,
        int fallbackCount,
        boolean humanReviewRequired,
        String qualityReviewStatus,
        String qualityReviewSummary,
        String summary,
        List<ControlledComparisonResult> results,
        List<String> reproducibilityNotes,
        List<String> limitations) {
}
