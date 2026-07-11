package com.example.aijobs.resume.dto;

import java.util.List;

public record ResumeOptimizationResponse(Long resumeId, Long jobId, String modelName, String overallSummary,
                                         List<String> matchedKeywords, List<String> missingKeywords,
                                         List<String> contentSuggestions, List<String> actionPlan) {
}
