package com.example.aijobs.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record ApplicationFollowUpAdviceResponse(Long applicationId,
                                                Long jobId,
                                                Long studentId,
                                                Long resumeId,
                                                String currentStatus,
                                                String modelName,
                                                BigDecimal matchScore,
                                                String scoreSource,
                                                String priorityLevel,
                                                String decisionSummary,
                                                String nextStatusSuggestion,
                                                List<String> matchedKeywords,
                                                List<String> missingKeywords,
                                                List<String> riskAlerts,
                                                List<String> recommendedActions,
                                                List<String> communicationTips) {
}
