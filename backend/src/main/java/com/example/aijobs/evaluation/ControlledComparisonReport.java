package com.example.aijobs.evaluation;

import java.util.List;

public record ControlledComparisonReport(
        String evaluationId,
        String runMode,
        String ruleModelName,
        String dictionaryVersion,
        int scenarioCount,
        int generatedCount,
        int fallbackCount,
        boolean humanReviewRequired,
        String summary,
        List<ControlledComparisonResult> results,
        List<String> limitations) {
}
