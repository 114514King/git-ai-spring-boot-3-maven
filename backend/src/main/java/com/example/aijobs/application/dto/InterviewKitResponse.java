package com.example.aijobs.application.dto;

import java.util.List;

public record InterviewKitResponse(Long applicationId,
                                   Long jobId,
                                   Long resumeId,
                                   Long studentId,
                                   String modelName,
                                   String summary,
                                   List<InterviewQuestion> questions,
                                   List<ScoringDimension> scoringDimensions,
                                   List<String> riskFocus,
                                   List<String> followUpSuggestions) {
    public record InterviewQuestion(String category,
                                    String question,
                                    String evaluationFocus,
                                    String expectedEvidence) {
    }

    public record ScoringDimension(String name,
                                   Integer weight,
                                   String highScoreSignal,
                                   String lowScoreRisk) {
    }
}
