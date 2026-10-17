package com.example.aijobs.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResumeStatusRequest(
        @NotBlank @Pattern(regexp = "DRAFT|PUBLISHED") String status) {
}
