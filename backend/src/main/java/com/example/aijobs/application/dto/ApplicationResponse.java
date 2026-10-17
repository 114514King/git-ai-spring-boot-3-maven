package com.example.aijobs.application.dto;

import com.example.aijobs.application.entity.JobApplication;

import java.time.LocalDateTime;

public record ApplicationResponse(Long id, Long jobId, Long studentId, Long resumeId, String status,
                                  LocalDateTime appliedAt, LocalDateTime updatedAt) {
    public static ApplicationResponse from(JobApplication application) {
        return new ApplicationResponse(application.getId(), application.getJobId(), application.getStudentId(),
                application.getResumeId(), application.getStatus(), application.getAppliedAt(),
                application.getUpdatedAt());
    }
}
