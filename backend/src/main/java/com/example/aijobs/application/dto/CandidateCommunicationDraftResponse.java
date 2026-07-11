package com.example.aijobs.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record CandidateCommunicationDraftResponse(
        Long applicationId,
        Long jobId,
        Long studentId,
        Long resumeId,
        String applicationStatus,
        String modelName,
        BigDecimal matchScore,
        String scoreSource,
        String communicationScenario,
        String subject,
        String openingMessage,
        List<String> keyQuestions,
        List<String> riskNotes,
        List<String> nextActions
) {
}
