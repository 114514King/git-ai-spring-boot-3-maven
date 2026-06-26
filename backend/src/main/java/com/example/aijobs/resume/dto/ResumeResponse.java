package com.example.aijobs.resume.dto;

import com.example.aijobs.resume.entity.Resume;

import java.time.LocalDateTime;

public record ResumeResponse(Long id, Long studentId, String title, String education,
                             String workExperience, String projectExperience, String skills,
                             String selfEvaluation, String status, LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
    public static ResumeResponse from(Resume resume) {
        return new ResumeResponse(resume.getId(), resume.getStudentId(), resume.getTitle(), resume.getEducation(),
                resume.getWorkExperience(), resume.getProjectExperience(), resume.getSkills(),
                resume.getSelfEvaluation(), resume.getStatus(), resume.getCreatedAt(), resume.getUpdatedAt());
    }
}
