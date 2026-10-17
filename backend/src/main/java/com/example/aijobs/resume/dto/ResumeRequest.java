package com.example.aijobs.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResumeRequest(
        @NotBlank @Size(max = 100) String title,
        String education,
        String workExperience,
        String projectExperience,
        String skills,
        String selfEvaluation) {
}
