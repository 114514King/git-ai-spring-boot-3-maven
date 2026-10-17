package com.example.aijobs.job.dto;

import com.example.aijobs.job.entity.JobPosting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record JobResponse(Long id, Long hrId, String title, String companyName, String city,
                          String employmentType, BigDecimal salaryMin, BigDecimal salaryMax,
                          String description, String requirements, String status,
                          LocalDateTime publishedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static JobResponse from(JobPosting job) {
        return new JobResponse(job.getId(), job.getHrId(), job.getTitle(), job.getCompanyName(), job.getCity(),
                job.getEmploymentType(), job.getSalaryMin(), job.getSalaryMax(), job.getDescription(),
                job.getRequirements(), job.getStatus(), job.getPublishedAt(), job.getCreatedAt(), job.getUpdatedAt());
    }
}
