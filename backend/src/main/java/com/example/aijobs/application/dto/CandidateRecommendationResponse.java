package com.example.aijobs.application.dto;

import com.example.aijobs.application.entity.JobApplication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CandidateRecommendationResponse(Long applicationId,
                                              Long jobId,
                                              Long studentId,
                                              Long resumeId,
                                              String applicationStatus,
                                              BigDecimal recommendationScore,
                                              String scoreSource,
                                              List<String> matchedKeywords,
                                              List<String> missingKeywords,
                                              String recommendationReason,
                                              String riskSummary,
                                              String suggestedAction,
                                              LocalDateTime appliedAt) {
    public static CandidateRecommendationResponse from(JobApplication application,
                                                       BigDecimal recommendationScore,
                                                       String scoreSource,
                                                       List<String> matchedKeywords,
                                                       List<String> missingKeywords,
                                                       String recommendationReason,
                                                       String riskSummary,
                                                       String suggestedAction) {
        return new CandidateRecommendationResponse(application.getId(), application.getJobId(),
                application.getStudentId(), application.getResumeId(), application.getStatus(),
                recommendationScore, scoreSource, matchedKeywords, missingKeywords, recommendationReason,
                riskSummary, suggestedAction, application.getAppliedAt());
    }
}
